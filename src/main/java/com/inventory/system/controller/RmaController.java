package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.ReturnAuthorizationDto;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.service.RmaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class RmaController {

    private final RmaService rmaService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnAuthorizationDto>> createRma(@Valid @RequestBody CreateRmaRequest request) {
        ReturnAuthorizationDto rma = rmaService.createRma(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "RMA created successfully", rma), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnAuthorizationDto>> getRma(@PathVariable UUID id) {
        ReturnAuthorizationDto rma = rmaService.getRma(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "RMA retrieved successfully", rma), HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReturnAuthorizationDto>> updateRmaStatus(@PathVariable UUID id, @Valid @RequestBody UpdateRmaStatusRequest request) {
        ReturnAuthorizationDto rma = rmaService.updateRmaStatus(id, request);
        return new ResponseEntity<>(new ApiResponse<>(true, "RMA status updated successfully", rma), HttpStatus.OK);
    }
}
