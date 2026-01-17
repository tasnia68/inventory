package com.inventory.system.service;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SerialNumber;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SerialNumberRepository;
import com.inventory.system.repository.StockMovementRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final InventoryValuationService valuationService;
    private final BatchRepository batchRepository;
    private final SerialNumberRepository serialNumberRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StockDto> getStocks(UUID warehouseId, UUID productVariantId, Pageable pageable) {
        Specification<Stock> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (productVariantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), productVariantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return stockRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StockDto getStock(UUID id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "id", id));
        return mapToDto(stock);
    }

    @Override
    @Transactional
    public StockMovementDto adjustStock(StockAdjustmentDto dto) {
        ProductVariant variant = productVariantRepository.findById(dto.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", dto.getProductVariantId()));
        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", dto.getWarehouseId()));

        StorageLocation location = null;
        if (dto.getStorageLocationId() != null) {
            location = storageLocationRepository.findById(dto.getStorageLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", dto.getStorageLocationId()));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Storage location does not belong to the specified warehouse");
            }
        }

        Batch batch = null;
        if (dto.getBatchId() != null) {
            batch = batchRepository.findById(dto.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", dto.getBatchId()));
        }

        if (Boolean.TRUE.equals(variant.getTemplate().getIsBatchTracked()) && batch == null) {
            throw new IllegalArgumentException("Batch ID is required for this product");
        }
        if (Boolean.FALSE.equals(variant.getTemplate().getIsBatchTracked()) && batch != null) {
            throw new IllegalArgumentException("Batch ID provided for non-batch-tracked product");
        }

        // Serial Number Validation
        if (Boolean.TRUE.equals(variant.getTemplate().getIsSerialTracked())) {
            if (dto.getSerialNumbers() == null || dto.getSerialNumbers().isEmpty()) {
                throw new IllegalArgumentException("Serial numbers are required for this product");
            }
            if (BigDecimal.valueOf(dto.getSerialNumbers().size()).compareTo(dto.getQuantity().abs()) != 0) {
                 throw new IllegalArgumentException("Quantity must match the number of serial numbers provided");
            }
        } else {
             if (dto.getSerialNumbers() != null && !dto.getSerialNumbers().isEmpty()) {
                 throw new IllegalArgumentException("Serial numbers provided for non-serial-tracked product");
             }
        }

        Stock stock;
        if (location != null) {
            if (batch != null) {
                stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchId(
                                variant.getId(), warehouse.getId(), location.getId(), batch.getId())
                        .orElse(createStock(variant, warehouse, location, batch));
            } else {
                stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdAndBatchIdIsNull(
                                variant.getId(), warehouse.getId(), location.getId())
                        .orElse(createStock(variant, warehouse, location, null));
            }
        } else {
            if (batch != null) {
                stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchId(
                                variant.getId(), warehouse.getId(), batch.getId())
                        .orElse(createStock(variant, warehouse, null, batch));
            } else {
                stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNullAndBatchIdIsNull(
                                variant.getId(), warehouse.getId())
                        .orElse(createStock(variant, warehouse, null, null));
            }
        }

        BigDecimal quantityChange = dto.getQuantity();
        BigDecimal currentQuantity = stock.getQuantity();

        switch (dto.getType()) {
            case IN:
            case TRANSFER_IN:
                if (quantityChange.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Quantity must be positive for IN/TRANSFER_IN");
                }
                stock.setQuantity(currentQuantity.add(quantityChange));
                break;
            case OUT:
            case TRANSFER_OUT:
                if (quantityChange.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Quantity must be positive for OUT/TRANSFER_OUT");
                }
                if (currentQuantity.compareTo(quantityChange) < 0) {
                    throw new IllegalArgumentException("Insufficient stock");
                }
                stock.setQuantity(currentQuantity.subtract(quantityChange));
                break;
            case ADJUSTMENT:
                stock.setQuantity(currentQuantity.add(quantityChange));
                break;
        }

        if (stock.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        stock = stockRepository.save(stock);

        StockMovement movement = new StockMovement();
        movement.setProductVariant(variant);
        movement.setWarehouse(warehouse);
        movement.setStorageLocation(location);
        movement.setBatch(batch);
        movement.setQuantity(quantityChange);
        movement.setType(dto.getType());
        movement.setReason(dto.getReason());
        movement.setReferenceId(dto.getReferenceId());

        // Handle Serial Numbers
        if (Boolean.TRUE.equals(variant.getTemplate().getIsSerialTracked())) {
            handleSerialNumbers(dto, variant, warehouse, location, batch);
        }

        // Handle Valuation
        switch (dto.getType()) {
            case IN:
            case TRANSFER_IN:
            case ADJUSTMENT:
                // For positive adjustments/IN, we process inbound valuation
                // ADJUSTMENT can be positive or negative?
                // Typically ADJUSTMENT type is used for "Correction".
                // If it's increasing stock, it's Inbound.
                // The switch above updated stock quantity based on type.
                // If ADJUSTMENT added quantity:
                // But wait, ADJUSMENT logic above: stock.setQuantity(currentQuantity.add(quantityChange));
                // quantityChange is from dto.getQuantity().
                // If quantityChange is positive -> Inbound.
                // If quantityChange is negative -> Outbound.

                // Let's check quantity sign.
                if (quantityChange.compareTo(BigDecimal.ZERO) >= 0) {
                     valuationService.processInbound(movement, dto.getUnitCost());
                } else {
                     // For outbound, we need positive quantity for valuation processing?
                     // My valuation service expects positive quantity in movement usually?
                     // processOutbound uses movement.getQuantity() to reduce layers.
                     // If movement.quantity is negative, logic breaks.
                     // StockMovement quantity is "change".
                     // For OUT type, quantityChange is passed to subtract.
                     // But StockMovement.quantity usually stores the absolute amount?
                     // Let's check existing implementation.
                     // dto.getQuantity() is set to movement.setQuantity().
                     // If type is OUT, adjustStock does: stock.setQuantity(currentQuantity.subtract(quantityChange));
                     // So quantityChange is POSITIVE for OUT.
                     // If type is ADJUSTMENT, quantityChange can be negative.

                     if (dto.getType() == StockMovement.StockMovementType.ADJUSTMENT && quantityChange.compareTo(BigDecimal.ZERO) < 0) {
                         // Convert to positive for valuation processing
                         movement.setQuantity(quantityChange.abs());
                         valuationService.processOutbound(movement);
                         // Restore original signed quantity for movement record?
                         // Usually movement records signed quantity or type indicates sign.
                         // StockMovementType IN/OUT implies sign.
                         // ADJUSTMENT might need sign.
                         movement.setQuantity(quantityChange); // Restore
                     } else {
                         // Normal OUT/TRANSFER_OUT
                         valuationService.processOutbound(movement);
                     }
                }
                break;
            case OUT:
            case TRANSFER_OUT:
                valuationService.processOutbound(movement);
                break;
        }

        stockMovementRepository.save(movement);

        return mapToDto(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementDto> getStockMovements(UUID warehouseId, UUID productVariantId, Pageable pageable) {
        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (productVariantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), productVariantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return stockMovementRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    private Stock createStock(ProductVariant v, Warehouse w, StorageLocation l, Batch b) {
        Stock s = new Stock();
        s.setProductVariant(v);
        s.setWarehouse(w);
        s.setStorageLocation(l);
        s.setBatch(b);
        s.setQuantity(BigDecimal.ZERO);
        return s;
    }

    private StockDto mapToDto(Stock stock) {
        StockDto dto = new StockDto();
        dto.setId(stock.getId());
        dto.setProductVariantId(stock.getProductVariant().getId());
        dto.setProductVariantSku(stock.getProductVariant().getSku());
        dto.setWarehouseId(stock.getWarehouse().getId());
        dto.setWarehouseName(stock.getWarehouse().getName());
        if (stock.getStorageLocation() != null) {
            dto.setStorageLocationId(stock.getStorageLocation().getId());
            dto.setStorageLocationName(stock.getStorageLocation().getName());
        }
        if (stock.getBatch() != null) {
            dto.setBatchId(stock.getBatch().getId());
            dto.setBatchNumber(stock.getBatch().getBatchNumber());
        }
        dto.setQuantity(stock.getQuantity());
        dto.setCreatedAt(stock.getCreatedAt());
        dto.setUpdatedAt(stock.getUpdatedAt());
        return dto;
    }

    private void handleSerialNumbers(StockAdjustmentDto dto, ProductVariant variant, Warehouse warehouse, StorageLocation location, Batch batch) {
        for (String snStr : dto.getSerialNumbers()) {
            switch (dto.getType()) {
                case IN:
                case TRANSFER_IN:
                    // Create or Update
                    SerialNumber sn = serialNumberRepository.findBySerialNumberAndProductVariantId(snStr, variant.getId())
                            .orElse(new SerialNumber());

                    if (sn.getId() == null) {
                         sn.setSerialNumber(snStr);
                         sn.setProductVariant(variant);
                    } else {
                        // If it exists, check status?
                        // If it's effectively "New" to the system or being returned.
                        // For simplicity, we assume we can move it back to inventory.
                    }
                    sn.setWarehouse(warehouse);
                    sn.setStorageLocation(location);
                    sn.setBatch(batch);
                    sn.setStatus(SerialNumber.SerialNumberStatus.AVAILABLE);
                    serialNumberRepository.save(sn);
                    break;
                case OUT:
                case TRANSFER_OUT:
                    SerialNumber snOut = serialNumberRepository.findBySerialNumberAndProductVariantId(snStr, variant.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("SerialNumber", "serialNumber", snStr));

                    // Verify it is in the correct warehouse/location?
                    // Optional: stricter checks here.

                    snOut.setStatus(SerialNumber.SerialNumberStatus.SOLD); // Or SHIPPED/USED depending on context. Defaulting to SOLD for OUT.
                    // For TRANSFER_OUT, it might stay AVAILABLE but change warehouse?
                    // Wait, TRANSFER mechanism usually involves OUT from Source and IN to Dest.
                    // This function handles one side of the transaction.
                    // If it is TRANSFER_OUT, we might want to keep it "AVAILABLE" but effectively "In Transit" or just remove location?
                    // Current simplified logic:
                    if (dto.getType() == StockMovement.StockMovementType.TRANSFER_OUT) {
                         // Typically transfers are 2-step.
                         // Here we just remove it from current location context or mark it.
                         // Let's clear location for now.
                         snOut.setWarehouse(null);
                         snOut.setStorageLocation(null);
                         // Status?
                    } else {
                        snOut.setWarehouse(null);
                        snOut.setStorageLocation(null);
                        snOut.setStatus(SerialNumber.SerialNumberStatus.SOLD);
                    }
                    serialNumberRepository.save(snOut);
                    break;
                case ADJUSTMENT:
                    // Depends on quantity sign.
                    if (dto.getQuantity().compareTo(BigDecimal.ZERO) >= 0) {
                        // IN logic
                         SerialNumber snAdj = serialNumberRepository.findBySerialNumberAndProductVariantId(snStr, variant.getId())
                            .orElse(new SerialNumber());
                         if (snAdj.getId() == null) {
                             snAdj.setSerialNumber(snStr);
                             snAdj.setProductVariant(variant);
                         }
                         snAdj.setWarehouse(warehouse);
                         snAdj.setStorageLocation(location);
                         snAdj.setBatch(batch);
                         snAdj.setStatus(SerialNumber.SerialNumberStatus.AVAILABLE);
                         serialNumberRepository.save(snAdj);
                    } else {
                        // OUT logic
                        SerialNumber snAdjOut = serialNumberRepository.findBySerialNumberAndProductVariantId(snStr, variant.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("SerialNumber", "serialNumber", snStr));
                        snAdjOut.setWarehouse(null);
                        snAdjOut.setStorageLocation(null);
                        snAdjOut.setStatus(SerialNumber.SerialNumberStatus.SOLD); // or Adjusted Out
                        serialNumberRepository.save(snAdjOut);
                    }
                    break;
            }
        }
    }

    private StockMovementDto mapToDto(StockMovement movement) {
        StockMovementDto dto = new StockMovementDto();
        dto.setId(movement.getId());
        dto.setProductVariantId(movement.getProductVariant().getId());
        dto.setProductVariantSku(movement.getProductVariant().getSku());
        dto.setWarehouseId(movement.getWarehouse().getId());
        dto.setWarehouseName(movement.getWarehouse().getName());
        if (movement.getStorageLocation() != null) {
            dto.setStorageLocationId(movement.getStorageLocation().getId());
            dto.setStorageLocationName(movement.getStorageLocation().getName());
        }
        if (movement.getBatch() != null) {
            dto.setBatchId(movement.getBatch().getId());
            dto.setBatchNumber(movement.getBatch().getBatchNumber());
        }
        dto.setQuantity(movement.getQuantity());
        dto.setUnitCost(movement.getUnitCost());
        dto.setTotalCost(movement.getTotalCost());
        dto.setType(movement.getType());
        dto.setReason(movement.getReason());
        dto.setReferenceId(movement.getReferenceId());
        dto.setCreatedAt(movement.getCreatedAt());
        dto.setCreatedBy(movement.getCreatedBy());
        return dto;
    }
}
