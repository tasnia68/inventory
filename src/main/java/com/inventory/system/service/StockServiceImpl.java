package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.repository.ProductVariantRepository;
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
    public StockDto adjustStock(StockAdjustmentDto dto) {
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

        Stock stock;
        if (location != null) {
            stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationId(
                    variant.getId(), warehouse.getId(), location.getId())
                    .orElse(createStock(variant, warehouse, location));
        } else {
            stock = stockRepository.findByProductVariantIdAndWarehouseIdAndStorageLocationIdIsNull(
                    variant.getId(), warehouse.getId())
                    .orElse(createStock(variant, warehouse, null));
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
        movement.setQuantity(quantityChange);
        movement.setType(dto.getType());
        movement.setReason(dto.getReason());
        movement.setReferenceId(dto.getReferenceId());
        stockMovementRepository.save(movement);

        return mapToDto(stock);
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

    private Stock createStock(ProductVariant v, Warehouse w, StorageLocation l) {
        Stock s = new Stock();
        s.setProductVariant(v);
        s.setWarehouse(w);
        s.setStorageLocation(l);
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
        dto.setQuantity(stock.getQuantity());
        dto.setCreatedAt(stock.getCreatedAt());
        dto.setUpdatedAt(stock.getUpdatedAt());
        return dto;
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
        dto.setQuantity(movement.getQuantity());
        dto.setType(movement.getType());
        dto.setReason(movement.getReason());
        dto.setReferenceId(movement.getReferenceId());
        dto.setCreatedAt(movement.getCreatedAt());
        dto.setCreatedBy(movement.getCreatedBy());
        return dto;
    }
}
