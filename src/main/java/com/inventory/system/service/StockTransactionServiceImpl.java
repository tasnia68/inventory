package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StockTransaction;
import com.inventory.system.common.entity.StockTransactionItem;
import com.inventory.system.common.entity.StockTransactionStatus;
import com.inventory.system.common.entity.StockTransactionType;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.payload.StockTransactionItemDto;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.StockTransactionRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final StockService stockService;

    @Override
    @Transactional
    public StockTransactionDto createTransaction(CreateStockTransactionRequest request) {
        StockTransaction transaction = new StockTransaction();
        transaction.setTransactionNumber(generateTransactionNumber());
        transaction.setType(request.getType());
        transaction.setStatus(StockTransactionStatus.DRAFT);
        transaction.setReference(request.getReference());
        transaction.setNotes(request.getNotes());
        transaction.setTransactionDate(LocalDateTime.now());

        if (request.getSourceWarehouseId() != null) {
            Warehouse source = warehouseRepository.findById(request.getSourceWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getSourceWarehouseId()));
            transaction.setSourceWarehouse(source);
        }

        if (request.getDestinationWarehouseId() != null) {
            Warehouse dest = warehouseRepository.findById(request.getDestinationWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getDestinationWarehouseId()));
            transaction.setDestinationWarehouse(dest);
        }

        // Validate Warehouse requirements based on Type
        validateWarehouses(transaction);

        List<StockTransactionItem> items = new ArrayList<>();
        for (CreateStockTransactionRequest.ItemRequest itemRequest : request.getItems()) {
            StockTransactionItem item = new StockTransactionItem();
            item.setStockTransaction(transaction);

            ProductVariant variant = productVariantRepository.findById(itemRequest.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", itemRequest.getProductVariantId()));
            item.setProductVariant(variant);

            item.setQuantity(itemRequest.getQuantity());

            if (itemRequest.getSourceStorageLocationId() != null) {
                StorageLocation loc = storageLocationRepository.findById(itemRequest.getSourceStorageLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", itemRequest.getSourceStorageLocationId()));
                item.setSourceStorageLocation(loc);
            }

            if (itemRequest.getDestinationStorageLocationId() != null) {
                StorageLocation loc = storageLocationRepository.findById(itemRequest.getDestinationStorageLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", itemRequest.getDestinationStorageLocationId()));
                item.setDestinationStorageLocation(loc);
            }

            items.add(item);
        }
        transaction.setItems(items);

        transaction = stockTransactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    @Override
    @Transactional
    public StockTransactionDto confirmTransaction(UUID id) {
        StockTransaction transaction = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", id));

        if (transaction.getStatus() == StockTransactionStatus.COMPLETED) {
            throw new BadRequestException("Transaction is already completed");
        }
        if (transaction.getStatus() == StockTransactionStatus.CANCELLED) {
            throw new BadRequestException("Cannot confirm a cancelled transaction");
        }

        processStockMovements(transaction);

        transaction.setStatus(StockTransactionStatus.COMPLETED);
        transaction = stockTransactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    @Override
    @Transactional
    public StockTransactionDto cancelTransaction(UUID id) {
        StockTransaction transaction = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", id));

        if (transaction.getStatus() == StockTransactionStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed transaction");
        }

        transaction.setStatus(StockTransactionStatus.CANCELLED);
        transaction = stockTransactionRepository.save(transaction);
        return mapToDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransactionDto getTransaction(UUID id) {
        StockTransaction transaction = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", id));
        return mapToDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransactionDto> getTransactions(Pageable pageable) {
        return stockTransactionRepository.findAll(pageable).map(this::mapToDto);
    }

    private void validateWarehouses(StockTransaction transaction) {
        switch (transaction.getType()) {
            case INBOUND:
                if (transaction.getDestinationWarehouse() == null) {
                    throw new BadRequestException("Destination warehouse is required for INBOUND");
                }
                break;
            case OUTBOUND:
                if (transaction.getSourceWarehouse() == null) {
                    throw new BadRequestException("Source warehouse is required for OUTBOUND");
                }
                break;
            case TRANSFER:
                if (transaction.getSourceWarehouse() == null || transaction.getDestinationWarehouse() == null) {
                    throw new BadRequestException("Source and Destination warehouses are required for TRANSFER");
                }
                break;
            case ADJUSTMENT:
                if (transaction.getSourceWarehouse() == null) {
                    throw new BadRequestException("Source warehouse is required for ADJUSTMENT");
                }
                break;
        }
    }

    private void processStockMovements(StockTransaction transaction) {
        for (StockTransactionItem item : transaction.getItems()) {
            switch (transaction.getType()) {
                case INBOUND:
                    StockAdjustmentDto inbound = new StockAdjustmentDto();
                    inbound.setProductVariantId(item.getProductVariant().getId());
                    inbound.setWarehouseId(transaction.getDestinationWarehouse().getId());
                    if (item.getDestinationStorageLocation() != null) {
                        inbound.setStorageLocationId(item.getDestinationStorageLocation().getId());
                    }
                    inbound.setQuantity(item.getQuantity());
                    inbound.setType(StockMovement.StockMovementType.IN);
                    inbound.setReason("Transaction: " + transaction.getTransactionNumber());
                    inbound.setReferenceId(transaction.getId().toString());
                    stockService.adjustStock(inbound);
                    break;

                case OUTBOUND:
                    StockAdjustmentDto outbound = new StockAdjustmentDto();
                    outbound.setProductVariantId(item.getProductVariant().getId());
                    outbound.setWarehouseId(transaction.getSourceWarehouse().getId());
                    if (item.getSourceStorageLocation() != null) {
                        outbound.setStorageLocationId(item.getSourceStorageLocation().getId());
                    }
                    outbound.setQuantity(item.getQuantity());
                    outbound.setType(StockMovement.StockMovementType.OUT);
                    outbound.setReason("Transaction: " + transaction.getTransactionNumber());
                    outbound.setReferenceId(transaction.getId().toString());
                    stockService.adjustStock(outbound);
                    break;

                case TRANSFER:
                    // Out from Source
                    StockAdjustmentDto transferOut = new StockAdjustmentDto();
                    transferOut.setProductVariantId(item.getProductVariant().getId());
                    transferOut.setWarehouseId(transaction.getSourceWarehouse().getId());
                    if (item.getSourceStorageLocation() != null) {
                        transferOut.setStorageLocationId(item.getSourceStorageLocation().getId());
                    }
                    transferOut.setQuantity(item.getQuantity());
                    transferOut.setType(StockMovement.StockMovementType.TRANSFER_OUT);
                    transferOut.setReason("Transfer: " + transaction.getTransactionNumber());
                    transferOut.setReferenceId(transaction.getId().toString());
                    stockService.adjustStock(transferOut);

                    // In to Destination
                    StockAdjustmentDto transferIn = new StockAdjustmentDto();
                    transferIn.setProductVariantId(item.getProductVariant().getId());
                    transferIn.setWarehouseId(transaction.getDestinationWarehouse().getId());
                    if (item.getDestinationStorageLocation() != null) {
                        transferIn.setStorageLocationId(item.getDestinationStorageLocation().getId());
                    }
                    transferIn.setQuantity(item.getQuantity());
                    transferIn.setType(StockMovement.StockMovementType.TRANSFER_IN);
                    transferIn.setReason("Transfer: " + transaction.getTransactionNumber());
                    transferIn.setReferenceId(transaction.getId().toString());
                    stockService.adjustStock(transferIn);
                    break;

                case ADJUSTMENT:
                    StockAdjustmentDto adjustment = new StockAdjustmentDto();
                    adjustment.setProductVariantId(item.getProductVariant().getId());
                    adjustment.setWarehouseId(transaction.getSourceWarehouse().getId());
                    if (item.getSourceStorageLocation() != null) {
                        adjustment.setStorageLocationId(item.getSourceStorageLocation().getId());
                    }
                    adjustment.setQuantity(item.getQuantity());
                    adjustment.setType(StockMovement.StockMovementType.ADJUSTMENT);
                    adjustment.setReason("Adjustment: " + transaction.getTransactionNumber());
                    adjustment.setReferenceId(transaction.getId().toString());
                    stockService.adjustStock(adjustment);
                    break;
            }
        }
    }

    private String generateTransactionNumber() {
        return "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private StockTransactionDto mapToDto(StockTransaction transaction) {
        StockTransactionDto dto = new StockTransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionNumber(transaction.getTransactionNumber());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        if (transaction.getSourceWarehouse() != null) {
            dto.setSourceWarehouseId(transaction.getSourceWarehouse().getId());
            dto.setSourceWarehouseName(transaction.getSourceWarehouse().getName());
        }
        if (transaction.getDestinationWarehouse() != null) {
            dto.setDestinationWarehouseId(transaction.getDestinationWarehouse().getId());
            dto.setDestinationWarehouseName(transaction.getDestinationWarehouse().getName());
        }
        dto.setReference(transaction.getReference());
        dto.setNotes(transaction.getNotes());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setCreatedBy(transaction.getCreatedBy());

        dto.setItems(transaction.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private StockTransactionItemDto mapItemToDto(StockTransactionItem item) {
        StockTransactionItemDto dto = new StockTransactionItemDto();
        dto.setId(item.getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setQuantity(item.getQuantity());
        if (item.getSourceStorageLocation() != null) {
            dto.setSourceStorageLocationId(item.getSourceStorageLocation().getId());
            dto.setSourceStorageLocationName(item.getSourceStorageLocation().getName());
        }
        if (item.getDestinationStorageLocation() != null) {
            dto.setDestinationStorageLocationId(item.getDestinationStorageLocation().getId());
            dto.setDestinationStorageLocationName(item.getDestinationStorageLocation().getName());
        }
        return dto;
    }
}
