package com.inventory.system.controller;

import com.inventory.system.common.entity.DeliveryZone;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ShippingRateCardDto;
import com.inventory.system.payload.ShippingRateCardRequest;
import com.inventory.system.service.ShippingRateCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShippingRateCardController {

    private final ShippingRateCardService shippingRateCardService;

    @PostMapping("/courier-profiles/{profileId}/rate-cards")
    public ResponseEntity<ApiResponse<ShippingRateCardDto>> create(
            @PathVariable UUID profileId,
            @Valid @RequestBody ShippingRateCardRequest request) {
        ShippingRateCardDto dto = shippingRateCardService.create(profileId, request);
        return new ResponseEntity<>(ApiResponse.success(dto, "Rate card created"), HttpStatus.CREATED);
    }

    @PutMapping("/rate-cards/{id}")
    public ResponseEntity<ApiResponse<ShippingRateCardDto>> update(@PathVariable UUID id, @Valid @RequestBody ShippingRateCardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingRateCardService.update(id, request), "Rate card updated"));
    }

    @GetMapping("/courier-profiles/{profileId}/rate-cards")
    public ResponseEntity<ApiResponse<List<ShippingRateCardDto>>> listByProfile(@PathVariable UUID profileId) {
        return ResponseEntity.ok(ApiResponse.success(shippingRateCardService.listByProfile(profileId), "OK"));
    }

    @GetMapping("/courier-profiles/{profileId}/rate-cards/by-zone")
    public ResponseEntity<ApiResponse<ShippingRateCardDto>> findByZone(
            @PathVariable UUID profileId,
            @RequestParam DeliveryZone zone) {
        ShippingRateCardDto dto = shippingRateCardService.findByProfileAndZone(profileId, zone)
                .orElseThrow(() -> new ResourceNotFoundException("ShippingRateCard", "zone", zone));
        return ResponseEntity.ok(ApiResponse.success(dto, "OK"));
    }

    @DeleteMapping("/rate-cards/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shippingRateCardService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Rate card deleted"));
    }

    @GetMapping("/delivery-zones")
    public ResponseEntity<ApiResponse<List<String>>> listZones() {
        List<String> zones = java.util.Arrays.stream(DeliveryZone.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success(zones, "OK"));
    }
}
