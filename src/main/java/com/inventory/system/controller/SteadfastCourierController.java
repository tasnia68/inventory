package com.inventory.system.controller;

import com.inventory.system.common.entity.ShipmentQueueType;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ShipmentQueueRefreshResultDto;
import com.inventory.system.service.SteadfastCourierService;
import com.inventory.system.service.courier.CourierReturnResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier/steadfast")
@RequiredArgsConstructor
public class SteadfastCourierController {

    private final SteadfastCourierService steadfastCourierService;

    @PostMapping("/shipments/{shipmentId}/book")
    public ResponseEntity<ApiResponse<Object>> bookShipment(@PathVariable UUID shipmentId) {
        Object result = steadfastCourierService.bookShipment(shipmentId);
        return ResponseEntity.ok(ApiResponse.success(result, "Shipment booked with Steadfast successfully"));
    }

    @PostMapping("/shipments/{shipmentId}/sync-status")
    public ResponseEntity<ApiResponse<Object>> syncStatus(@PathVariable UUID shipmentId) {
        Object result = steadfastCourierService.syncStatus(shipmentId);
        return ResponseEntity.ok(ApiResponse.success(result, "Steadfast status synced successfully"));
    }

    @PostMapping("/queue/{queue}/refresh")
    public ResponseEntity<ApiResponse<ShipmentQueueRefreshResultDto>> refreshQueue(
            @PathVariable ShipmentQueueType queue,
            @RequestParam(defaultValue = "50") int batchSize) {
        ShipmentQueueRefreshResultDto result = steadfastCourierService.refreshQueue(queue, batchSize);
        return ResponseEntity.ok(ApiResponse.success(result, "Steadfast queue refreshed successfully"));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<SteadfastCourierService.SteadfastBalanceResponse>> getBalance() {
        var balance = steadfastCourierService.getBalance();
        return ResponseEntity.ok(ApiResponse.success(balance, "Steadfast balance retrieved"));
    }

    @PostMapping("/shipments/{shipmentId}/return-request")
    public ResponseEntity<ApiResponse<CourierReturnResult>> requestReturn(
            @PathVariable UUID shipmentId,
            @RequestParam(required = false) String reason) {
        CourierReturnResult result = steadfastCourierService.requestReturn(shipmentId, reason);
        return ResponseEntity.ok(ApiResponse.success(result, "Steadfast return request submitted"));
    }

    @PostMapping("/sync-payments")
    public ResponseEntity<ApiResponse<SteadfastCourierService.PaymentSyncResult>> syncPayments() {
        var result = steadfastCourierService.syncPayments();
        return ResponseEntity.ok(ApiResponse.success(result, "Steadfast payments synced"));
    }
}
