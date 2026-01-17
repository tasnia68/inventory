package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.StockReservationDto;
import com.inventory.system.payload.StockReservationRequest;
import com.inventory.system.service.StockReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-reservations")
@RequiredArgsConstructor
public class StockReservationController {

    private final StockReservationService stockReservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockReservationDto>> reserveStock(@Valid @RequestBody StockReservationRequest request) {
        StockReservationDto reservation = stockReservationService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StockReservationDto>builder()
                        .success(true)
                        .status(HttpStatus.CREATED.value())
                        .message("Stock reserved successfully")
                        .data(reservation)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @PutMapping("/{id}/release")
    public ResponseEntity<ApiResponse<Void>> releaseReservation(@PathVariable UUID id) {
        stockReservationService.releaseReservation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Reservation released successfully"));
    }

    @GetMapping("/atp")
    public ResponseEntity<ApiResponse<BigDecimal>> getAvailableToPromise(
            @RequestParam UUID productVariantId,
            @RequestParam UUID warehouseId) {
        BigDecimal atp = stockReservationService.getAvailableToPromise(productVariantId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(atp, "Available to Promise retrieved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockReservationDto>>> getReservations(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productVariantId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StockReservationDto> reservations = stockReservationService.getReservations(warehouseId, productVariantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reservations, "Reservations retrieved successfully"));
    }
}
