package com.inventory.system.service;

import com.inventory.system.payload.StockReservationDto;
import com.inventory.system.payload.StockReservationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockReservationService {
    StockReservationDto reserveStock(StockReservationRequest request);
    void releaseReservation(UUID reservationId);
    void releaseReservationsByReference(String referenceId);
    void fulfillReservationsByReference(String referenceId);
    BigDecimal getAvailableToPromise(UUID productVariantId, UUID warehouseId);
    void cleanupExpiredReservations();
    Page<StockReservationDto> getReservations(UUID warehouseId, UUID productVariantId, Pageable pageable);
}
