package com.inventory.system.service;

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
import java.util.List;
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
    private final StockService stockService;

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
    public CycleCountDto startCycleCount(UUID id) {
        CycleCount cycleCount = getCycleCountEntity(id);
        if (cycleCount.getStatus() != CycleCountStatus.DRAFT && cycleCount.getStatus() != CycleCountStatus.ASSIGNED) {
            throw new IllegalStateException("Cycle count must be in DRAFT or ASSIGNED state to start");
        }

        if (cycleCount.getType() != CycleCountType.FULL) {
            throw new UnsupportedOperationException("Partial and Spot Check counts are not yet implemented. Please use FULL count.");
        }

        cycleCount.setStatus(CycleCountStatus.IN_PROGRESS);
        cycleCountRepository.save(cycleCount);

        // Snapshot stock
        // FULL count logic: get all stock in warehouse

        // Use specification to filter by warehouse
        stockRepository.findAll((root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), cycleCount.getWarehouse().getId())
        ).forEach(stock -> {
            CycleCountItem item = new CycleCountItem();
            item.setCycleCount(cycleCount);
            item.setProductVariant(stock.getProductVariant());
            item.setStorageLocation(stock.getStorageLocation());
            item.setBatch(stock.getBatch());
            item.setSystemQuantity(stock.getQuantity());
            item.setCountedQuantity(BigDecimal.ZERO); // Default to 0 or null? Let's say 0.
            item.setVariance(BigDecimal.ZERO.subtract(stock.getQuantity()));
            cycleCountItemRepository.save(item);
        });

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
            item.setCountedQuantity(entry.getCountedQuantity());
            if (entry.getNotes() != null) {
                item.setNotes(entry.getNotes());
            }
            // Recalculate variance immediately or wait for finish?
            // Let's recalculate immediately for UI feedback if needed
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
            // Create new item (unexpected item found)
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
            item.setSystemQuantity(BigDecimal.ZERO); // It wasn't in snapshot
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
                // Adjust stock
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
                adjustment.setQuantity(item.getVariance());
                adjustment.setReason("Cycle Count Adjustment: " + cycleCount.getReference());
                adjustment.setReferenceId(cycleCount.getReference());

                if (Boolean.TRUE.equals(item.getProductVariant().getTemplate().getIsSerialTracked())) {
                     // Skipping adjustment for serial tracked items for now to avoid crash, or throw error?
                     // Let's throw error to be safe.
                     throw new UnsupportedOperationException("Automatic adjustment for serial-tracked items via Cycle Count is not yet supported. Please adjust manually.");
                }

                stockService.adjustStock(adjustment);
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
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
