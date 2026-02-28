package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.UpdateShipmentStatusRequest;
import com.inventory.system.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentDto>> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentDto shipment = shipmentService.createShipment(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Shipment created successfully", shipment), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentDto>> getShipment(@PathVariable UUID id) {
        ShipmentDto shipment = shipmentService.getShipment(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Shipment retrieved successfully", shipment), HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShipmentDto>> updateShipmentStatus(@PathVariable UUID id, @Valid @RequestBody UpdateShipmentStatusRequest request) {
        ShipmentDto shipment = shipmentService.updateShipmentStatus(id, request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Shipment status updated successfully", shipment), HttpStatus.OK);
    }

    @GetMapping("/{id}/label")
    public ResponseEntity<ApiResponse<String>> getShipmentLabel(@PathVariable UUID id) {
        String label = shipmentService.generateLabel(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Shipment label generated successfully", label), HttpStatus.OK);
    }

    @GetMapping("/{id}/delivery-note")
    public ResponseEntity<ApiResponse<String>> getDeliveryNote(@PathVariable UUID id) {
        String note = shipmentService.generateDeliveryNote(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Delivery note generated successfully", note), HttpStatus.OK);
    }
}
