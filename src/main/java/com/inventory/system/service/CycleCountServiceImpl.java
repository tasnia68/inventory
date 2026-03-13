package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CycleCountServiceImpl implements CycleCountService {

    private final CycleCountRepository cycleCountRepository;
    private final CycleCountItemRepository cycleCountItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final BatchRepository batchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CycleCountDto createCycleCount(CreateCycleCountRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        CycleCount cycleCount = new CycleCount();
        cycleCount.setWarehouse(warehouse);
        cycleCount.setType(request.getType());
        cycleCount.setDueDate(request.getDueDate());
        cycleCount.setDescription(request.getDescription());
        cycleCount.setStatus(CycleCountStatus.DRAFT);
        cycleCount.setReference(generateReference());

        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedUserId()));
            cycleCount.setAssignedUser(user);
        }

        return mapToDto(cycleCountRepository.save(cycleCount));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CycleCountDto> getCycleCounts(Pageable pageable) {
        return cycleCountRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CycleCountDto getCycleCount(UUID id) {
        return mapToDto(getCycleCountEntity(id));
    }

    @Override
    @Transactional
    public CycleCountDto updateCycleCount(UUID id, UpdateCycleCountRequest request) {
        CycleCount cycleCount = getCycleCountEntity(id);

        if (request.getStatus() != null) {
            // Add state transition logic if needed
             cycleCount.setStatus(request.getStatus());
        }
        if (request.getAssignedUserId() != null) {
             User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedUserId()));
            cycleCount.setAssignedUser(user);
        }
        if (request.getDueDate() != null) {
            cycleCount.setDueDate(request.getDueDate());
        }
        if (request.getDescription() != null) {
            cycleCount.setDescription(request.getDescription());
        }

        return mapToDto(cycleCountRepository.save(cycleCount));
    }

    @Override
    @Transactional
    public CycleCountDto scheduleCycleCount(UUID id, ScheduleCycleCountRequest request) {
        CycleCount cycleCount = getCycleCountEntity(id);

        if (request.getDueDate() != null) {
            cycleCount.setDueDate(request.getDueDate());
        }
        if (request.getDescription() != null) {
            cycleCount.setDescription(request.getDescription());
        }
        if (request.getAssignedUserId() != null) {
            User user = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedUserId()));
            cycleCount.setAssignedUser(user);
        }

        cycleCount.setStatus(CycleCountStatus.ASSIGNED);
        return mapToDto(cycleCountRepository.save(cycleCount));
    }

    @Override
    @Transactional
    public CycleCountDto startCycleCount(UUID id) {
        CycleCount cycleCount = getCycleCountEntity(id);
        if (cycleCount.getStatus() != CycleCountStatus.DRAFT && cycleCount.getStatus() != CycleCountStatus.ASSIGNED) {
            throw new IllegalStateException("Cycle count must be in DRAFT or ASSIGNED state to start");
        }

        cycleCount.setStatus(CycleCountStatus.IN_PROGRESS);
        cycleCountRepository.save(cycleCount);

        if (cycleCount.getType() == CycleCountType.FULL) {
            stockRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("warehouse").get("id"), cycleCount.getWarehouse().getId())
            ).forEach(stock -> cycleCountItemRepository.save(buildSnapshotItem(cycleCount, stock)));
        }

        return mapToDto(cycleCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CycleCountItemDto> getCycleCountItems(UUID id) {
        return cycleCountItemRepository.findByCycleCountId(id).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void enterCount(UUID id, List<CycleCountEntryRequest> entries) {
        CycleCount cycleCount = getCycleCountEntity(id);
        if (cycleCount.getStatus() != CycleCountStatus.IN_PROGRESS) {
             throw new IllegalStateException("Cycle count must be IN_PROGRESS to enter counts");
        }

        for (CycleCountEntryRequest entry : entries) {
            CycleCountItem item = findOrCreateItem(cycleCount, entry);
            validateEntry(item, entry);
            item.setCountedQuantity(entry.getCountedQuantity());
            item.setCountedSerialNumbers(serializeSerialNumbers(entry.getSerialNumbers()));
            if (entry.getNotes() != null) {
                item.setNotes(entry.getNotes());
            }
            BigDecimal systemQty = item.getSystemQuantity() != null ? item.getSystemQuantity() : BigDecimal.ZERO;
            item.setVariance(item.getCountedQuantity().subtract(systemQty));
            cycleCountItemRepository.save(item);
        }
    }

    private CycleCountItem findOrCreateItem(CycleCount cycleCount, CycleCountEntryRequest entry) {
        // Try to find existing item
        // We need to handle null location/batch carefully
        UUID variantId = entry.getProductVariantId();
        UUID locationId = entry.getStorageLocationId();
        UUID batchId = entry.getBatchId();

        CycleCountItem item = null;
        if (locationId != null) {
            if (batchId != null) {
                item = cycleCountItemRepository.findByCycleCountIdAndProductVariantIdAndStorageLocationIdAndBatchId(
                        cycleCount.getId(), variantId, locationId, batchId).orElse(null);
            } else {
                 item = cycleCountItemRepository.findByCycleCountIdAndProductVariantIdAndStorageLocationIdAndBatchIdIsNull(
                        cycleCount.getId(), variantId, locationId).orElse(null);
            }
        } else {
            if (batchId != null) {
                 item = cycleCountItemRepository.findByCycleCountIdAndProductVariantIdAndStorageLocationIdIsNullAndBatchId(
                        cycleCount.getId(), variantId, batchId).orElse(null);
            } else {
                 item = cycleCountItemRepository.findByCycleCountIdAndProductVariantIdAndStorageLocationIdIsNullAndBatchIdIsNull(
                        cycleCount.getId(), variantId).orElse(null);
            }
        }

        if (item == null) {
            item = new CycleCountItem();
            item.setCycleCount(cycleCount);
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));
            item.setProductVariant(variant);

            if (locationId != null) {
                StorageLocation location = storageLocationRepository.findById(locationId)
                        .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", locationId));
                 item.setStorageLocation(location);
            }
            if (batchId != null) {
                Batch batch = batchRepository.findById(batchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
                item.setBatch(batch);
            }
            item.setSystemQuantity(resolveSystemQuantity(cycleCount.getWarehouse().getId(), variantId, locationId, batchId));
            item.setSystemSerialNumbers(serializeSerialNumbers(resolveSystemSerialNumbers(
                    variant,
                    cycleCount.getWarehouse().getId(),
                    locationId,
                    batchId
            )));
            item.setCountedQuantity(BigDecimal.ZERO);
            item.setVariance(BigDecimal.ZERO.subtract(item.getSystemQuantity()));
        }
        return item;
    }

    @Override
    @Transactional
    public CycleCountDto finishCycleCount(UUID id) {
        CycleCount cycleCount = getCycleCountEntity(id);
        if (cycleCount.getStatus() != CycleCountStatus.IN_PROGRESS) {
             throw new IllegalStateException("Cycle count must be IN_PROGRESS to finish");
        }
        cycleCount.setStatus(CycleCountStatus.REVIEW);
        return mapToDto(cycleCountRepository.save(cycleCount));
    }

    @Override
    @Transactional
    public CycleCountDto approveCycleCount(UUID id) {
        CycleCount cycleCount = getCycleCountEntity(id);
        if (cycleCount.getStatus() != CycleCountStatus.REVIEW) {
             throw new IllegalStateException("Cycle count must be in REVIEW to approve");
        }

        List<CycleCountItem> items = cycleCountItemRepository.findByCycleCountId(id);
        for (CycleCountItem item : items) {
            if (item.getVariance().compareTo(BigDecimal.ZERO) != 0) {
                if (Boolean.TRUE.equals(item.getProductVariant().getTemplate().getIsSerialTracked())) {
                    applySerialTrackedAdjustment(cycleCount, item);
                } else {
                    stockService.adjustStock(buildAdjustment(cycleCount, item, item.getVariance(), null));
                }
            }
        }

        cycleCount.setStatus(CycleCountStatus.COMPLETED);
        cycleCount.setCompletionDate(LocalDate.now());
        return mapToDto(cycleCountRepository.save(cycleCount));
    }

    private CycleCount getCycleCountEntity(UUID id) {
        return cycleCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CycleCount", "id", id));
    }

    private String generateReference() {
        return "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CycleCountDto mapToDto(CycleCount entity) {
        CycleCountDto dto = new CycleCountDto();
        dto.setId(entity.getId());
        dto.setReference(entity.getReference());
        dto.setWarehouseId(entity.getWarehouse().getId());
        dto.setWarehouseName(entity.getWarehouse().getName());
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getType());
        dto.setDueDate(entity.getDueDate());
        dto.setCompletionDate(entity.getCompletionDate());
        dto.setDescription(entity.getDescription());
        if (entity.getAssignedUser() != null) {
            dto.setAssignedUserId(entity.getAssignedUser().getId());
            // Assuming User has name/email
            dto.setAssignedUserName(entity.getAssignedUser().getEmail());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private CycleCountItemDto mapToDto(CycleCountItem entity) {
        CycleCountItemDto dto = new CycleCountItemDto();
        dto.setId(entity.getId());
        dto.setCycleCountId(entity.getCycleCount().getId());
        dto.setProductVariantId(entity.getProductVariant().getId());
        dto.setProductVariantSku(entity.getProductVariant().getSku());
        if (entity.getProductVariant().getTemplate() != null) {
            dto.setProductVariantName(entity.getProductVariant().getTemplate().getName());
        } else {
            dto.setProductVariantName(entity.getProductVariant().getSku());
        }
        if (entity.getStorageLocation() != null) {
            dto.setStorageLocationId(entity.getStorageLocation().getId());
            dto.setStorageLocationName(entity.getStorageLocation().getName());
        }
        if (entity.getBatch() != null) {
            dto.setBatchId(entity.getBatch().getId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());
        }
        dto.setSystemQuantity(entity.getSystemQuantity());
        dto.setCountedQuantity(entity.getCountedQuantity());
        dto.setVariance(entity.getVariance());
        dto.setSystemSerialNumbers(deserializeSerialNumbers(entity.getSystemSerialNumbers()));
        dto.setCountedSerialNumbers(deserializeSerialNumbers(entity.getCountedSerialNumbers()));
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private CycleCountItem buildSnapshotItem(CycleCount cycleCount, Stock stock) {
        CycleCountItem item = new CycleCountItem();
        item.setCycleCount(cycleCount);
        item.setProductVariant(stock.getProductVariant());
        item.setStorageLocation(stock.getStorageLocation());
        item.setBatch(stock.getBatch());
        item.setSystemQuantity(stock.getQuantity());
        item.setCountedQuantity(BigDecimal.ZERO);
        item.setVariance(BigDecimal.ZERO.subtract(stock.getQuantity()));
        item.setSystemSerialNumbers(serializeSerialNumbers(resolveSystemSerialNumbers(
                stock.getProductVariant(),
                cycleCount.getWarehouse().getId(),
                stock.getStorageLocation() != null ? stock.getStorageLocation().getId() : null,
                stock.getBatch() != null ? stock.getBatch().getId() : null
        )));
        return item;
    }

    private void validateEntry(CycleCountItem item, CycleCountEntryRequest entry) {
        if (entry.getCountedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted quantity cannot be negative");
        }

        if (!Boolean.TRUE.equals(item.getProductVariant().getTemplate().getIsSerialTracked())) {
            return;
        }

        if (entry.getCountedQuantity().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Serial-tracked items require an integer counted quantity");
        }

        List<String> serialNumbers = normalizeSerialNumbers(entry.getSerialNumbers());
        if (serialNumbers.size() != entry.getCountedQuantity().intValueExact()) {
            throw new IllegalArgumentException("Serial numbers count must match counted quantity for serial-tracked items");
        }
    }

    private StockAdjustmentDto buildAdjustment(
            CycleCount cycleCount,
            CycleCountItem item,
            BigDecimal quantity,
            List<String> serialNumbers) {
        StockAdjustmentDto adjustment = new StockAdjustmentDto();
        adjustment.setProductVariantId(item.getProductVariant().getId());
        adjustment.setWarehouseId(cycleCount.getWarehouse().getId());
        if (item.getStorageLocation() != null) {
            adjustment.setStorageLocationId(item.getStorageLocation().getId());
        }
        if (item.getBatch() != null) {
            adjustment.setBatchId(item.getBatch().getId());
        }
        adjustment.setType(StockMovement.StockMovementType.ADJUSTMENT);
        adjustment.setQuantity(quantity);
        adjustment.setReason("Cycle Count Adjustment: " + cycleCount.getReference());
        adjustment.setReferenceId(cycleCount.getReference());
        adjustment.setSerialNumbers(serialNumbers);
        return adjustment;
    }

    private void applySerialTrackedAdjustment(CycleCount cycleCount, CycleCountItem item) {
        List<String> systemSerials = normalizeSerialNumbers(deserializeSerialNumbers(item.getSystemSerialNumbers()));
        List<String> countedSerials = normalizeSerialNumbers(deserializeSerialNumbers(item.getCountedSerialNumbers()));

        if (countedSerials.size() != item.getCountedQuantity().intValueExact()) {
            throw new IllegalStateException("Counted serial numbers do not match counted quantity for item " + item.getId());
        }

        Set<String> systemSet = new LinkedHashSet<>(systemSerials);
        Set<String> countedSet = new LinkedHashSet<>(countedSerials);

        List<String> missingSerials = systemSet.stream()
                .filter(serial -> !countedSet.contains(serial))
                .collect(Collectors.toList());
        List<String> addedSerials = countedSet.stream()
                .filter(serial -> !systemSet.contains(serial))
                .collect(Collectors.toList());

        if (!missingSerials.isEmpty()) {
            stockService.adjustStock(buildAdjustment(cycleCount, item, BigDecimal.valueOf(-missingSerials.size()), missingSerials));
        }

        for (String serialNumber : addedSerials) {
            relocateOrAddSerial(cycleCount, item, serialNumber);
        }
    }

    private void relocateOrAddSerial(CycleCount cycleCount, CycleCountItem targetItem, String serialNumber) {
        SerialNumber existingSerial = serialNumberRepository
                .findBySerialNumberAndProductVariantId(serialNumber, targetItem.getProductVariant().getId())
                .orElse(null);

        if (existingSerial != null && existingSerial.getWarehouse() != null
                && !isSameInventoryPosition(existingSerial, cycleCount.getWarehouse().getId(), targetItem)) {
            CycleCountItem sourceItem = new CycleCountItem();
            sourceItem.setProductVariant(targetItem.getProductVariant());
            sourceItem.setStorageLocation(existingSerial.getStorageLocation());
            sourceItem.setBatch(existingSerial.getBatch());
            stockService.adjustStock(buildAdjustment(cycleCount, sourceItem, BigDecimal.valueOf(-1), List.of(serialNumber)));
        }

        stockService.adjustStock(buildAdjustment(cycleCount, targetItem, BigDecimal.ONE, List.of(serialNumber)));
    }

    private boolean isSameInventoryPosition(SerialNumber serialNumber, UUID warehouseId, CycleCountItem item) {
        if (serialNumber.getWarehouse() == null || !serialNumber.getWarehouse().getId().equals(warehouseId)) {
            return false;
        }

        UUID serialLocationId = serialNumber.getStorageLocation() != null ? serialNumber.getStorageLocation().getId() : null;
        UUID itemLocationId = item.getStorageLocation() != null ? item.getStorageLocation().getId() : null;
        UUID serialBatchId = serialNumber.getBatch() != null ? serialNumber.getBatch().getId() : null;
        UUID itemBatchId = item.getBatch() != null ? item.getBatch().getId() : null;

        return java.util.Objects.equals(serialLocationId, itemLocationId)
                && java.util.Objects.equals(serialBatchId, itemBatchId);
    }

    private BigDecimal resolveSystemQuantity(UUID warehouseId, UUID variantId, UUID locationId, UUID batchId) {
        if (locationId != null) {
            if (batchId != null) {
                return stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchId(variantId, warehouseId, locationId, batchId)
                        .map(Stock::getQuantity)
                        .orElse(BigDecimal.ZERO);
            }
            return stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdIsNull(variantId, warehouseId, locationId)
                    .map(Stock::getQuantity)
                    .orElse(BigDecimal.ZERO);
        }

        if (batchId != null) {
            return stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchId(variantId, warehouseId, batchId)
                    .map(Stock::getQuantity)
                    .orElse(BigDecimal.ZERO);
        }

        return stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(variantId, warehouseId)
                .map(Stock::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    private List<String> resolveSystemSerialNumbers(ProductVariant variant, UUID warehouseId, UUID locationId, UUID batchId) {
        if (!Boolean.TRUE.equals(variant.getTemplate().getIsSerialTracked())) {
            return List.of();
        }

        return serialNumberRepository.findByInventoryPosition(
                        variant.getId(),
                        warehouseId,
                        locationId,
                        batchId,
                        SerialNumberStatus.AVAILABLE
                ).stream()
                .map(SerialNumber::getSerialNumber)
                .collect(Collectors.toList());
    }

    private String serializeSerialNumbers(List<String> serialNumbers) {
        try {
            return objectMapper.writeValueAsString(normalizeSerialNumbers(serialNumbers));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize cycle count serial numbers", exception);
        }
    }

    private List<String> deserializeSerialNumbers(String serialNumbers) {
        if (serialNumbers == null || serialNumbers.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(serialNumbers, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize cycle count serial numbers", exception);
        }
    }

    private List<String> normalizeSerialNumbers(List<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            return List.of();
        }

        List<String> cleaned = serialNumbers.stream()
                .filter(serial -> serial != null && !serial.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));

        if (cleaned.size() != new LinkedHashSet<>(cleaned).size()) {
            throw new IllegalArgumentException("Serial numbers must be unique within a cycle count entry");
        }

        return cleaned;
    }
}
