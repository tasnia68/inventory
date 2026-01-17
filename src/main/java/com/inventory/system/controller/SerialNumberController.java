package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/serial-numbers")
@RequiredArgsConstructor
public class SerialNumberController {

    private final StockService stockService;

    @GetMapping("/{serialNumber}/history")
    public ResponseEntity<ApiResponse<List<StockMovementDto>>> getHistory(@PathVariable String serialNumber) {
        List<StockMovementDto> history = stockService.getSerialNumberHistory(serialNumber);
        ApiResponse<List<StockMovementDto>> response = new ApiResponse<>(true, "Serial number history retrieved successfully", history);
        return ResponseEntity.ok(response);
    }
}
