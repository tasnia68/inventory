package com.inventory.system.service;

import com.inventory.system.common.entity.Batch;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.StockReservation;
import com.inventory.system.common.entity.StockReservationStatus;
import com.inventory.system.common.entity.StorageLocation;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.StockReservationDto;
import com.inventory.system.payload.StockReservationRequest;
import com.inventory.system.repository.BatchRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.StockReservationRepository;
import com.inventory.system.repository.StorageLocationRepository;
import com.inventory.system.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationServiceImpl implements StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final StockRepository stockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final BatchRepository batchRepository;

    @Override
    @Transactional
    public StockReservationDto reserveStock(StockReservationRequest request) {
        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", request.getProductVariantId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        StorageLocation location = null;
        if (request.getStorageLocationId() != null) {
            location = storageLocationRepository.findById(request.getStorageLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("StorageLocation", "id", request.getStorageLocationId()));
            if (!location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Storage location does not belong to the specified warehouse");
            }
        }

        Batch batch = null;
        if (request.getBatchId() != null) {
            batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));
        }

        // Check ATP
        BigDecimal atp = getAvailableToPromise(variant.getId(), warehouse.getId());
        if (atp.compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("Insufficient stock available to promise. ATP: " + atp);
        }

        StockReservation reservation = new StockReservation();
        reservation.setProductVariant(variant);
        reservation.setWarehouse(warehouse);
        reservation.setStorageLocation(location);
        reservation.setBatch(batch);
        reservation.setQuantity(request.getQuantity());
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setExpiresAt(request.getExpiresAt());
        reservation.setStatus(StockReservationStatus.ACTIVE);
        reservation.setPriority(request.getPriority());
        reservation.setReferenceId(request.getReferenceId());
        reservation.setNotes(request.getNotes());

        reservation = stockReservationRepository.save(reservation);
        return mapToDto(reservation);
    }

    @Override
    @Transactional
    public void releaseReservation(UUID reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", "id", reservationId));

        if (reservation.getStatus() == StockReservationStatus.ACTIVE || reservation.getStatus() == StockReservationStatus.PENDING) {
            reservation.setStatus(StockReservationStatus.RELEASED);
            stockReservationRepository.save(reservation);
        } else {
             throw new IllegalStateException("Reservation is not in a releasable state: " + reservation.getStatus());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableToPromise(UUID productVariantId, UUID warehouseId) {
        BigDecimal totalStock = stockRepository.countTotalQuantityByProductVariantAndWarehouse(productVariantId, warehouseId);
        if (totalStock == null) {
            totalStock = BigDecimal.ZERO;
        }

        BigDecimal reservedStock = stockReservationRepository.countTotalReservedQuantity(productVariantId, warehouseId);
        if (reservedStock == null) {
            reservedStock = BigDecimal.ZERO;
        }

        return totalStock.subtract(reservedStock);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredReservations() {
        List<StockReservation> expiredReservations = stockReservationRepository.findByStatusAndExpiresAtBefore(StockReservationStatus.ACTIVE, LocalDateTime.now());
        for (StockReservation reservation : expiredReservations) {
            reservation.setStatus(StockReservationStatus.EXPIRED);
            log.info("Expiring reservation {}", reservation.getId());
        }
        stockReservationRepository.saveAll(expiredReservations);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockReservationDto> getReservations(UUID warehouseId, UUID productVariantId, Pageable pageable) {
        Specification<StockReservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (productVariantId != null) {
                predicates.add(cb.equal(root.get("productVariant").get("id"), productVariantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return stockReservationRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    private StockReservationDto mapToDto(StockReservation reservation) {
        StockReservationDto dto = new StockReservationDto();
        dto.setId(reservation.getId());
        dto.setProductVariantId(reservation.getProductVariant().getId());
        dto.setProductVariantSku(reservation.getProductVariant().getSku());
        dto.setWarehouseId(reservation.getWarehouse().getId());
        dto.setWarehouseName(reservation.getWarehouse().getName());

        if (reservation.getStorageLocation() != null) {
            dto.setStorageLocationId(reservation.getStorageLocation().getId());
            dto.setStorageLocationName(reservation.getStorageLocation().getName());
        }

        if (reservation.getBatch() != null) {
            dto.setBatchId(reservation.getBatch().getId());
            dto.setBatchNumber(reservation.getBatch().getBatchNumber());
        }

        dto.setQuantity(reservation.getQuantity());
        dto.setReservedAt(reservation.getReservedAt());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setStatus(reservation.getStatus());
        dto.setPriority(reservation.getPriority());
        dto.setReferenceId(reservation.getReferenceId());
        dto.setNotes(reservation.getNotes());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setCreatedBy(reservation.getCreatedBy());
        return dto;
    }
}
