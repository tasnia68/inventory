package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CourierProfileDto;
import com.inventory.system.payload.CourierProfileRequest;
import com.inventory.system.service.CourierProfileService;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier-profiles")
@RequiredArgsConstructor
public class CourierProfileController {

    private final CourierProfileService courierProfileService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourierProfileDto>> create(@Valid @RequestBody CourierProfileRequest request) {
        CourierProfileDto dto = courierProfileService.createProfile(request);
        return new ResponseEntity<>(ApiResponse.success(dto, "Courier profile created"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourierProfileDto>> update(@PathVariable UUID id, @Valid @RequestBody CourierProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courierProfileService.updateProfile(id, request), "Courier profile updated"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourierProfileDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(courierProfileService.getProfile(id), "OK"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourierProfileDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(courierProfileService.listProfiles(), "OK"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        courierProfileService.deleteProfile(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Courier profile deleted"));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(courierProfileService.getBalance(id), "Balance retrieved"));
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<String>>> listProviders() {
        return ResponseEntity.ok(ApiResponse.success(courierProfileService.listRegisteredProviderCodes(), "OK"));
    }
}
