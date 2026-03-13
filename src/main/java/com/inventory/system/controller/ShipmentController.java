package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.DeliveryConfirmationRequest;
import com.inventory.system.payload.DeliveryNoteDto;
import com.inventory.system.payload.GenerateShippingLabelRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentSearchRequest;
import com.inventory.system.payload.UpdateShipmentTrackingRequest;
import com.inventory.system.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentDto>> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentDto shipment = shipmentService.createShipment(request);
        return new ResponseEntity<>(ApiResponse.success(shipment, "Shipment created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentDto>> getShipment(@PathVariable UUID id) {
        ShipmentDto shipment = shipmentService.getShipment(id);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Shipment retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShipmentDto>>> getAllShipments(
            @RequestParam(required = false) UUID salesOrderId,
            @RequestParam(required = false) com.inventory.system.common.entity.ShipmentStatus status,
            @RequestParam(required = false) String shipmentNumber,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "shippedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        ShipmentSearchRequest searchRequest = new ShipmentSearchRequest();
        searchRequest.setSalesOrderId(salesOrderId);
        searchRequest.setStatus(status);
        searchRequest.setShipmentNumber(shipmentNumber);
        searchRequest.setTrackingNumber(trackingNumber);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);

        Page<ShipmentDto> shipments = shipmentService.getAllShipments(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(shipments, "Shipments retrieved successfully"));
    }

    @PatchMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<ShipmentDto>> updateTracking(@PathVariable UUID id,
                                                                    @RequestBody UpdateShipmentTrackingRequest request) {
        ShipmentDto shipment = shipmentService.updateTracking(id, request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Shipment tracking updated successfully"));
    }

    @PostMapping("/{id}/label")
    public ResponseEntity<ApiResponse<ShipmentDto>> generateShippingLabel(@PathVariable UUID id,
                                                                           @RequestBody(required = false) GenerateShippingLabelRequest request) {
        ShipmentDto shipment = shipmentService.generateShippingLabel(id, request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Shipping label generated successfully"));
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<ApiResponse<ShipmentDto>> confirmDelivery(@PathVariable UUID id,
                                                                     @RequestBody(required = false) DeliveryConfirmationRequest request) {
        ShipmentDto shipment = shipmentService.confirmDelivery(id, request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Delivery confirmed successfully"));
    }

    @GetMapping("/{id}/delivery-note")
    public ResponseEntity<ApiResponse<DeliveryNoteDto>> generateDeliveryNote(@PathVariable UUID id) {
        DeliveryNoteDto note = shipmentService.generateDeliveryNote(id);
        return ResponseEntity.ok(ApiResponse.success(note, "Delivery note generated successfully"));
    }
}