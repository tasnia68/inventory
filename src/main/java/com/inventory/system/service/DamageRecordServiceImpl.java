package com.inventory.system.service;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.common.entity.DamageDispositionType;
import com.inventory.system.common.entity.DamageRecord;
import com.inventory.system.common.entity.DamageRecordDocument;
import com.inventory.system.common.entity.DamageRecordItem;
import com.inventory.system.common.entity.DamageRecordStatus;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import com.inventory.system.common.entity.ReturnMerchandiseAuthorization;
import com.inventory.system.common.entity.ReturnMerchandiseItem;
import com.inventory.system.common.entity.ReturnMerchandiseStatus;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.SerialNumberStatus;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockStatus;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateDamageRecordRequest;
import com.inventory.system.payload.CreateDamageRecordFromGrnRequest;
import com.inventory.system.payload.CreateDamageRecordFromRmaRequest;
import com.inventory.system.payload.CreateSupplierClaimItemRequest;
import com.inventory.system.payload.CreateSupplierClaimRequest;
import com.inventory.system.payload.DamageRecordDto;
import com.inventory.system.payload.DamageRecordDocumentDto;
import com.inventory.system.payload.DamageRecordItemDto;
import com.inventory.system.payload.DamageRecordReportDto;
import com.inventory.system.payload.DamageRecordSummaryDto;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.DamageRecordDocumentRepository;
import com.inventory.system.repository.DamageRecordRepository;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.ReturnMerchandiseAuthorizationRepository;
import com.inventory.system.repository.SerialNumberRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.SupplierClaimRepository;
import com.inventory.system.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DamageRecordServiceImpl implements DamageRecordService {

    private final DamageRecordRepository damageRecordRepository;
    private final DamageRecordDocumentRepository damageRecordDocumentRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final ReturnMerchandiseAuthorizationRepository returnMerchandiseAuthorizationRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BatchRepository batchRepository;
    private final StockService stockService;
    private final SerialNumberRepository serialNumberRepository;
    private final FileStorageService fileStorageService;
    private final SupplierClaimService supplierClaimService;
    private final SupplierClaimRepository supplierClaimRepository;

    @Override
    @Transactional
    public DamageRecordDto createDamageRecord(CreateDamageRecordRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        StorageLocation quarantineLocation = null;
        if (request.getQuarantineLocationId() != null) {
            quarantineLocation = storageLocationRepository.findById(request.getQuarantineLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", request.getQuarantineLocationId()));
            validateLocationWarehouse(quarantineLocation, warehouse.getId(), "Quarantine location");
        }

        DamageRecord record = new DamageRecord();
        record.setRecordNumber(generateRecordNumber());
        record.setStatus(DamageRecordStatus.DRAFT);
        record.setSourceType(request.getSourceType());
        record.setReasonCode(request.getReasonCode());
        record.setWarehouse(warehouse);
        record.setQuarantineLocation(quarantineLocation);
        record.setReference(request.getReference());
        record.setNotes(request.getNotes());
        record.setDamageDate(LocalDateTime.now());

        List<DamageRecordItem> items = new ArrayList<>();
        boolean hasQuarantineDisposition = false;

        for (CreateDamageRecordRequest.ItemRequest itemRequest : request.getItems()) {
            DamageRecordItem item = new DamageRecordItem();
            item.setDamageRecord(record);

            ProductVariant productVariant = productVariantRepository.findById(itemRequest.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", itemRequest.getProductVariantId()));
            item.setProductVariant(productVariant);

            if (itemRequest.getBatchId() != null) {
                Batch batch = batchRepository.findById(itemRequest.getBatchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", itemRequest.getBatchId()));
                item.setBatch(batch);
            }

            if (itemRequest.getSourceStorageLocationId() != null) {
                StorageLocation sourceLocation = storageLocationRepository.findById(itemRequest.getSourceStorageLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", itemRequest.getSourceStorageLocationId()));
                validateLocationWarehouse(sourceLocation, warehouse.getId(), "Source storage location");
                item.setSourceStorageLocation(sourceLocation);
            }

            item.setQuantity(itemRequest.getQuantity());
            item.setDisposition(itemRequest.getDisposition());
            item.setSerialNumbers(joinSerialNumbers(itemRequest.getSerialNumbers()));

            validateSerialNumbers(itemRequest.getSerialNumbers(), productVariant, warehouse.getId(), itemRequest.getSourceStorageLocationId(), itemRequest.getBatchId(), itemRequest.getQuantity());

            if (itemRequest.getDisposition() == DamageDispositionType.QUARANTINE) {
                hasQuarantineDisposition = true;
            }

            items.add(item);
        }

        if (hasQuarantineDisposition && quarantineLocation == null) {
            throw new BadRequestException("Quarantine location is required when any item is routed to quarantine");
        }

        record.setItems(items);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDto createDamageRecordFromGoodsReceipt(UUID goodsReceiptNoteId, CreateDamageRecordFromGrnRequest request) {
        GoodsReceiptNote goodsReceiptNote = goodsReceiptNoteRepository.findById(goodsReceiptNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", goodsReceiptNoteId));

        if (goodsReceiptNote.getStatus() != GoodsReceiptNoteStatus.VERIFIED && goodsReceiptNote.getStatus() != GoodsReceiptNoteStatus.COMPLETED) {
            throw new BadRequestException("Damage intake can only be created from verified or completed GRNs");
        }

        DamageRecord record = buildSourceDamageRecord(
                goodsReceiptNote.getWarehouse(),
                request.getQuarantineLocationId(),
                com.inventory.system.common.entity.DamageRecordSourceType.RECEIVING,
                request.getReasonCode(),
                goodsReceiptNote.getGrnNumber(),
                mergeSourceNotes("Generated from GRN " + goodsReceiptNote.getGrnNumber(), request.getNotes())
        );

        List<DamageRecordItem> items = new ArrayList<>();
        boolean hasQuarantineDisposition = false;

        for (CreateDamageRecordFromGrnRequest.ItemRequest itemRequest : request.getItems()) {
            GoodsReceiptNoteItem sourceItem = goodsReceiptNote.getItems().stream()
                    .filter(item -> item.getId().equals(itemRequest.getGoodsReceiptNoteItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("GRN item does not belong to the selected goods receipt note: " + itemRequest.getGoodsReceiptNoteItemId()));

            BigDecimal maxQuantity = BigDecimal.valueOf(sourceItem.getRejectedQuantity());
            BigDecimal quantity = itemRequest.getQuantity() != null ? itemRequest.getQuantity() : maxQuantity;
            if (maxQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("GRN item has no rejected quantity available for damage intake: " + sourceItem.getId());
            }
            if (quantity.compareTo(maxQuantity) > 0) {
                throw new BadRequestException("Damage quantity cannot exceed rejected quantity for GRN item " + sourceItem.getId());
            }

            DamageRecordItem item = new DamageRecordItem();
            item.setDamageRecord(record);
            item.setProductVariant(sourceItem.getProductVariant());
            item.setQuantity(quantity);
            item.setDisposition(itemRequest.getDisposition());
            item.setSerialNumbers(null);
            items.add(item);

            if (itemRequest.getDisposition() == DamageDispositionType.QUARANTINE) {
                hasQuarantineDisposition = true;
            }
        }

        validateQuarantineLocationRequired(record, hasQuarantineDisposition);
        record.setItems(items);
        DamageRecord savedRecord = damageRecordRepository.save(record);
        createSupplierClaimFromGrnIfRequested(savedRecord, goodsReceiptNote, request, items);
        return mapToDto(savedRecord);
    }

    @Override
    @Transactional
    public DamageRecordDto createDamageRecordFromRma(UUID rmaId, CreateDamageRecordFromRmaRequest request) {
        ReturnMerchandiseAuthorization rma = returnMerchandiseAuthorizationRepository.findById(rmaId)
                .orElseThrow(() -> new ResourceNotFoundException("RMA", "id", rmaId));

        if (rma.getStatus() != ReturnMerchandiseStatus.RECEIVED && rma.getStatus() != ReturnMerchandiseStatus.COMPLETED) {
            throw new BadRequestException("Damage intake can only be created from received or completed RMAs");
        }

        DamageRecord record = buildSourceDamageRecord(
                rma.getSalesOrder().getWarehouse(),
                request.getQuarantineLocationId(),
                com.inventory.system.common.entity.DamageRecordSourceType.SALES_RETURN,
                request.getReasonCode(),
                rma.getRmaNumber(),
                mergeSourceNotes("Generated from RMA " + rma.getRmaNumber(), request.getNotes())
        );

        List<DamageRecordItem> items = new ArrayList<>();
        boolean hasQuarantineDisposition = false;

        for (CreateDamageRecordFromRmaRequest.ItemRequest itemRequest : request.getItems()) {
            ReturnMerchandiseItem sourceItem = rma.getItems().stream()
                    .filter(item -> item.getId().equals(itemRequest.getRmaItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("RMA item does not belong to the selected RMA: " + itemRequest.getRmaItemId()));

            BigDecimal maxQuantity = sourceItem.getQuantity();
            BigDecimal quantity = itemRequest.getQuantity() != null ? itemRequest.getQuantity() : maxQuantity;
            if (quantity.compareTo(maxQuantity) > 0) {
                throw new BadRequestException("Damage quantity cannot exceed returned quantity for RMA item " + sourceItem.getId());
            }

            DamageRecordItem item = new DamageRecordItem();
            item.setDamageRecord(record);
            item.setProductVariant(sourceItem.getProductVariant());
            item.setQuantity(quantity);
            item.setDisposition(itemRequest.getDisposition());
            item.setSerialNumbers(null);
            items.add(item);

            if (itemRequest.getDisposition() == DamageDispositionType.QUARANTINE) {
                hasQuarantineDisposition = true;
            }
        }

        validateQuarantineLocationRequired(record, hasQuarantineDisposition);
        record.setItems(items);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public DamageRecordDto getDamageRecord(UUID id) {
        return mapToDto(getDamageRecordEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DamageRecordDto> getDamageRecords(UUID warehouseId, DamageRecordStatus status, Pageable pageable) {
        Specification<DamageRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return damageRecordRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DamageRecordReportDto> getDamageReport(UUID warehouseId, DamageRecordStatus status, LocalDate fromDate, LocalDate toDate) {
        return findDamageRecords(warehouseId, status, fromDate, toDate).stream()
                .flatMap(record -> record.getItems().stream().map(item -> mapReportItem(record, item)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DamageRecordSummaryDto getDamageSummary(UUID warehouseId, LocalDate fromDate, LocalDate toDate) {
        List<DamageRecord> records = findDamageRecords(warehouseId, null, fromDate, toDate);

        DamageRecordSummaryDto dto = new DamageRecordSummaryDto();
        dto.setTotalRecords(records.size());
        dto.setDraftRecords(records.stream().filter(record -> record.getStatus() == DamageRecordStatus.DRAFT).count());
        dto.setPendingApprovalRecords(records.stream().filter(record -> record.getStatus() == DamageRecordStatus.PENDING_APPROVAL).count());
        dto.setApprovedRecords(records.stream().filter(record -> record.getStatus() == DamageRecordStatus.APPROVED).count());
        dto.setCompletedRecords(records.stream().filter(record -> record.getStatus() == DamageRecordStatus.COMPLETED).count());
        dto.setCancelledRecords(records.stream().filter(record -> record.getStatus() == DamageRecordStatus.CANCELLED).count());

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal quarantineQuantity = BigDecimal.ZERO;
        BigDecimal writeOffQuantity = BigDecimal.ZERO;

        for (DamageRecord record : records) {
            for (DamageRecordItem item : record.getItems()) {
                totalQuantity = totalQuantity.add(item.getQuantity());
                if (item.getDisposition() == DamageDispositionType.QUARANTINE) {
                    quarantineQuantity = quarantineQuantity.add(item.getQuantity());
                }
                if (item.getDisposition() == DamageDispositionType.WRITE_OFF) {
                    writeOffQuantity = writeOffQuantity.add(item.getQuantity());
                }
            }
        }

        dto.setTotalDamagedQuantity(totalQuantity);
        dto.setQuarantineQuantity(quarantineQuantity);
        dto.setWriteOffQuantity(writeOffQuantity);
        return dto;
    }

    @Override
    @Transactional
    public DamageRecordDto submitForApproval(UUID id) {
        DamageRecord record = getDamageRecordEntity(id);
        if (record.getStatus() != DamageRecordStatus.DRAFT) {
            throw new BadRequestException("Only draft damage records can be submitted for approval");
        }

        record.setStatus(DamageRecordStatus.PENDING_APPROVAL);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDto approveDamageRecord(UUID id) {
        DamageRecord record = getDamageRecordEntity(id);
        if (record.getStatus() != DamageRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only damage records pending approval can be approved");
        }

        record.setStatus(DamageRecordStatus.APPROVED);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDto rejectDamageRecord(UUID id) {
        DamageRecord record = getDamageRecordEntity(id);
        if (record.getStatus() != DamageRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only damage records pending approval can be rejected");
        }

        record.setStatus(DamageRecordStatus.REJECTED);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDto confirmDamageRecord(UUID id) {
        DamageRecord record = getDamageRecordEntity(id);

        if (record.getStatus() == DamageRecordStatus.COMPLETED) {
            throw new BadRequestException("Damage record is already completed");
        }
        if (record.getStatus() == DamageRecordStatus.CANCELLED) {
            throw new BadRequestException("Cannot confirm a cancelled damage record");
        }
        if (record.getStatus() == DamageRecordStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Damage record is pending approval");
        }
        if (record.getStatus() == DamageRecordStatus.REJECTED) {
            throw new BadRequestException("Cannot confirm a rejected damage record");
        }

        boolean hasWriteOff = record.getItems().stream().anyMatch(item -> item.getDisposition() == DamageDispositionType.WRITE_OFF);
        if (hasWriteOff && record.getStatus() != DamageRecordStatus.APPROVED) {
            throw new BadRequestException("Write-off damage records must be approved before confirmation");
        }

        for (DamageRecordItem item : record.getItems()) {
            processItem(record, item);
        }

        record.setStatus(DamageRecordStatus.COMPLETED);
        record.setCompletedAt(LocalDateTime.now());
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDto cancelDamageRecord(UUID id) {
        DamageRecord record = getDamageRecordEntity(id);
        if (record.getStatus() == DamageRecordStatus.COMPLETED) {
            throw new BadRequestException("Completed damage records cannot be cancelled");
        }
        if (record.getStatus() == DamageRecordStatus.REJECTED) {
            throw new BadRequestException("Rejected damage records cannot be cancelled");
        }

        record.setStatus(DamageRecordStatus.CANCELLED);
        return mapToDto(damageRecordRepository.save(record));
    }

    @Override
    @Transactional
    public DamageRecordDocumentDto uploadDocument(UUID damageRecordId, MultipartFile file, String documentType, String notes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Damage document file is required");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new BadRequestException("Damage document type is required");
        }

        DamageRecord damageRecord = getDamageRecordEntity(damageRecordId);
        String storagePath = fileStorageService.uploadFile(file, "damage-records/" + damageRecordId);

        DamageRecordDocument document = new DamageRecordDocument();
        document.setDamageRecord(damageRecord);
        document.setDocumentType(documentType.trim());
        document.setFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setStoragePath(storagePath);
        document.setNotes(notes);
        return mapDocumentToDto(damageRecordDocumentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DamageRecordDocumentDto> getDocuments(UUID damageRecordId) {
        if (!damageRecordRepository.existsById(damageRecordId)) {
            throw new ResourceNotFoundException("DamageRecord", "id", damageRecordId);
        }

        return damageRecordDocumentRepository.findByDamageRecordIdOrderByCreatedAtDesc(damageRecordId).stream()
                .map(this::mapDocumentToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DamageRecordDocumentDto getDocument(UUID documentId) {
        return mapDocumentToDto(getDocumentEntity(documentId));
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        DamageRecordDocument document = getDocumentEntity(documentId);
        fileStorageService.deleteFile(document.getStoragePath());
        damageRecordDocumentRepository.delete(document);
    }

    private void processItem(DamageRecord record, DamageRecordItem item) {
        List<String> serialNumbers = splitSerialNumbers(item.getSerialNumbers());

        if (item.getDisposition() == DamageDispositionType.QUARANTINE) {
            if (item.getSourceStorageLocation() != null) {
                StockAdjustmentDto moveOut = buildStockAdjustment(record, item, item.getSourceStorageLocation(), item.getQuantity(), StockMovement.StockMovementType.OUT, serialNumbers);
                stockService.adjustStock(moveOut);
            }

            StockAdjustmentDto moveIn = buildStockAdjustment(record, item, record.getQuarantineLocation(), item.getQuantity(), StockMovement.StockMovementType.IN, serialNumbers);
            moveIn.setStockStatus(StockStatus.QUARANTINE);
            stockService.adjustStock(moveIn);
            return;
        }

        if (item.getSourceStorageLocation() != null) {
            StockAdjustmentDto writeOff = buildStockAdjustment(record, item, item.getSourceStorageLocation(), item.getQuantity().negate(), StockMovement.StockMovementType.ADJUSTMENT, serialNumbers);
            stockService.adjustStock(writeOff);
        }

        for (String serialNumber : serialNumbers) {
            SerialNumber serial = serialNumberRepository.findBySerialNumberAndProductVariantId(serialNumber, item.getProductVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("SerialNumber", "serialNumber", serialNumber));
            serial.setStatus(SerialNumberStatus.DAMAGED);
            serialNumberRepository.save(serial);
        }
    }

    private StockAdjustmentDto buildStockAdjustment(DamageRecord record, DamageRecordItem item, StorageLocation location, BigDecimal quantity,
                                                    StockMovement.StockMovementType type, List<String> serialNumbers) {
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setWarehouseId(record.getWarehouse().getId());
        if (location != null) {
            dto.setStorageLocationId(location.getId());
        }
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
        }
        dto.setQuantity(quantity);
        dto.setType(type);
        dto.setReason(buildMovementReason(record, item));
        dto.setReferenceId(record.getId().toString());
        dto.setSerialNumbers(serialNumbers.isEmpty() ? null : serialNumbers);
        return dto;
    }

    private String buildMovementReason(DamageRecord record, DamageRecordItem item) {
        return "Damage record " + record.getRecordNumber() + " - " + record.getReasonCode() + " - " + item.getDisposition();
    }

    private void validateLocationWarehouse(StorageLocation location, UUID warehouseId, String label) {
        if (!location.getWarehouse().getId().equals(warehouseId)) {
            throw new BadRequestException(label + " must belong to the selected warehouse");
        }
    }

    private DamageRecord buildSourceDamageRecord(Warehouse warehouse, UUID quarantineLocationId,
                                                 com.inventory.system.common.entity.DamageRecordSourceType sourceType,
                                                 com.inventory.system.common.entity.DamageReasonCode reasonCode,
                                                 String reference,
                                                 String notes) {
        StorageLocation quarantineLocation = null;
        if (quarantineLocationId != null) {
            quarantineLocation = storageLocationRepository.findById(quarantineLocationId)
                    .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", quarantineLocationId));
            validateLocationWarehouse(quarantineLocation, warehouse.getId(), "Quarantine location");
        }

        DamageRecord record = new DamageRecord();
        record.setRecordNumber(generateRecordNumber());
        record.setStatus(DamageRecordStatus.DRAFT);
        record.setSourceType(sourceType);
        record.setReasonCode(reasonCode);
        record.setWarehouse(warehouse);
        record.setQuarantineLocation(quarantineLocation);
        record.setReference(reference);
        record.setNotes(notes);
        record.setDamageDate(LocalDateTime.now());
        return record;
    }

    private void validateQuarantineLocationRequired(DamageRecord record, boolean hasQuarantineDisposition) {
        if (hasQuarantineDisposition && record.getQuarantineLocation() == null) {
            throw new BadRequestException("Quarantine location is required when any item is routed to quarantine");
        }
    }

    private String mergeSourceNotes(String sourceNote, String requestNotes) {
        if (requestNotes == null || requestNotes.isBlank()) {
            return sourceNote;
        }
        return sourceNote + ". " + requestNotes;
    }

    private void validateSerialNumbers(List<String> serialNumbers, ProductVariant productVariant, UUID warehouseId,
                                       UUID sourceStorageLocationId, UUID batchId, BigDecimal quantity) {
        if (Boolean.TRUE.equals(productVariant.getTemplate().getIsSerialTracked())) {
            if (serialNumbers == null || serialNumbers.isEmpty()) {
                throw new BadRequestException("Serial numbers are required for serial-tracked products");
            }
            if (quantity.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Quantity must be an integer for serial-tracked products");
            }
            if (BigDecimal.valueOf(serialNumbers.size()).compareTo(quantity) != 0) {
                throw new BadRequestException("Serial number count must match item quantity");
            }

            List<SerialNumber> availableSerials = serialNumberRepository.findByInventoryPosition(
                    productVariant.getId(),
                    warehouseId,
                    sourceStorageLocationId,
                    batchId,
                    SerialNumberStatus.AVAILABLE
            );
            List<String> availableSet = availableSerials.stream().map(SerialNumber::getSerialNumber).toList();
            for (String serialNumber : serialNumbers) {
                if (!availableSet.contains(serialNumber)) {
                    throw new BadRequestException("Serial number not available in the selected inventory position: " + serialNumber);
                }
            }
            return;
        }

        if (serialNumbers != null && !serialNumbers.isEmpty()) {
            throw new BadRequestException("Serial numbers were provided for a non-serial-tracked product");
        }
    }

    private DamageRecord getDamageRecordEntity(UUID id) {
        return damageRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DamageRecord", "id", id));
    }

    private DamageRecordDocument getDocumentEntity(UUID documentId) {
        return damageRecordDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DamageRecordDocument", "id", documentId));
    }

    private List<DamageRecord> findDamageRecords(UUID warehouseId, DamageRecordStatus status, LocalDate fromDate, LocalDate toDate) {
        Specification<DamageRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("damageDate"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThan(root.get("damageDate"), toDate.plusDays(1).atStartOfDay()));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return damageRecordRepository.findAll(spec);
    }

    private String joinSerialNumbers(List<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) {
            return null;
        }
        return serialNumbers.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(","));
    }

    private List<String> splitSerialNumbers(String serialNumbers) {
        if (serialNumbers == null || serialNumbers.isBlank()) {
            return List.of();
        }
        return Arrays.stream(serialNumbers.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private DamageRecordDto mapToDto(DamageRecord record) {
        DamageRecordDto dto = new DamageRecordDto();
        dto.setId(record.getId());
        dto.setRecordNumber(record.getRecordNumber());
        dto.setStatus(record.getStatus());
        dto.setSourceType(record.getSourceType());
        dto.setReasonCode(record.getReasonCode());
        dto.setWarehouseId(record.getWarehouse().getId());
        dto.setWarehouseName(record.getWarehouse().getName());
        if (record.getQuarantineLocation() != null) {
            dto.setQuarantineLocationId(record.getQuarantineLocation().getId());
            dto.setQuarantineLocationName(record.getQuarantineLocation().getName());
        }
        dto.setReference(record.getReference());
        dto.setNotes(record.getNotes());
        supplierClaimRepository.findFirstByDamageRecordIdOrderByCreatedAtDesc(record.getId()).ifPresent(claim -> {
            dto.setSupplierClaimId(claim.getId());
            dto.setSupplierClaimNumber(claim.getClaimNumber());
        });
        dto.setDamageDate(record.getDamageDate());
        dto.setCompletedAt(record.getCompletedAt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setCreatedBy(record.getCreatedBy());
        dto.setItems(record.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private void createSupplierClaimFromGrnIfRequested(DamageRecord record, GoodsReceiptNote goodsReceiptNote,
                                                       CreateDamageRecordFromGrnRequest request,
                                                       List<DamageRecordItem> items) {
        if (!Boolean.TRUE.equals(request.getCreateSupplierClaim())) {
            return;
        }

        CreateSupplierClaimRequest claimRequest = new CreateSupplierClaimRequest();
        claimRequest.setDamageRecordId(record.getId());
        claimRequest.setReason(request.getSupplierClaimReason());
        claimRequest.setNotes(request.getSupplierClaimNotes());
        claimRequest.setItems(buildSupplierClaimItems(goodsReceiptNote, request, items));
        supplierClaimService.createSupplierClaim(goodsReceiptNote.getId(), claimRequest);
    }

    private List<CreateSupplierClaimItemRequest> buildSupplierClaimItems(GoodsReceiptNote goodsReceiptNote,
                                                                         CreateDamageRecordFromGrnRequest request,
                                                                         List<DamageRecordItem> items) {
        List<CreateSupplierClaimItemRequest> claimItems = new ArrayList<>();

        for (int index = 0; index < request.getItems().size(); index++) {
            CreateDamageRecordFromGrnRequest.ItemRequest requestItem = request.getItems().get(index);
            GoodsReceiptNoteItem sourceItem = goodsReceiptNote.getItems().stream()
                    .filter(item -> item.getId().equals(requestItem.getGoodsReceiptNoteItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("GRN item does not belong to the selected goods receipt note: " + requestItem.getGoodsReceiptNoteItemId()));

            CreateSupplierClaimItemRequest claimItem = new CreateSupplierClaimItemRequest();
            claimItem.setGoodsReceiptNoteItemId(sourceItem.getId());
            claimItem.setQuantity(toWholeQuantity(items.get(index).getQuantity(), sourceItem.getId()));
            claimItem.setReason(sourceItem.getRejectionReason());
            claimItems.add(claimItem);
        }

        return claimItems;
    }

    private int toWholeQuantity(BigDecimal quantity, UUID goodsReceiptNoteItemId) {
        try {
            return quantity.intValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Supplier claim linkage requires whole-number quantity for GRN item " + goodsReceiptNoteItemId);
        }
    }

    private DamageRecordItemDto mapItemToDto(DamageRecordItem item) {
        DamageRecordItemDto dto = new DamageRecordItemDto();
        dto.setId(item.getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
            dto.setBatchNumber(item.getBatch().getBatchNumber());
        }
        if (item.getSourceStorageLocation() != null) {
            dto.setSourceStorageLocationId(item.getSourceStorageLocation().getId());
            dto.setSourceStorageLocationName(item.getSourceStorageLocation().getName());
        }
        dto.setQuantity(item.getQuantity());
        dto.setDisposition(item.getDisposition());
        dto.setSerialNumbers(splitSerialNumbers(item.getSerialNumbers()));
        return dto;
    }

    private DamageRecordReportDto mapReportItem(DamageRecord record, DamageRecordItem item) {
        DamageRecordReportDto dto = new DamageRecordReportDto();
        dto.setDamageRecordId(record.getId());
        dto.setRecordNumber(record.getRecordNumber());
        dto.setStatus(record.getStatus());
        dto.setSourceType(record.getSourceType());
        dto.setReasonCode(record.getReasonCode());
        dto.setWarehouseId(record.getWarehouse().getId());
        dto.setWarehouseName(record.getWarehouse().getName());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setDisposition(item.getDisposition());
        dto.setQuantity(item.getQuantity());
        dto.setDamageDate(record.getDamageDate());
        return dto;
    }

    private DamageRecordDocumentDto mapDocumentToDto(DamageRecordDocument document) {
        DamageRecordDocumentDto dto = new DamageRecordDocumentDto();
        dto.setId(document.getId());
        dto.setDamageRecordId(document.getDamageRecord().getId());
        dto.setRecordNumber(document.getDamageRecord().getRecordNumber());
        dto.setDocumentType(document.getDocumentType());
        dto.setFilename(document.getFilename());
        dto.setContentType(document.getContentType());
        dto.setStoragePath(document.getStoragePath());
        dto.setNotes(document.getNotes());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }

    private String generateRecordNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "DMG-" + datePart + "-" + uuidPart;
    }
}