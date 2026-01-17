package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.service.StockTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-transactions")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransactionDto>> createTransaction(@Valid @RequestBody CreateStockTransactionRequest request) {
        StockTransactionDto transaction = stockTransactionService.createTransaction(request);
        return ResponseEntity.created(URI.create("/api/v1/stock-transactions/" + transaction.getId()))
                .body(ApiResponse.success(transaction, "Stock transaction created successfully"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<StockTransactionDto>> confirmTransaction(@PathVariable UUID id) {
        StockTransactionDto transaction = stockTransactionService.confirmTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Stock transaction confirmed successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<StockTransactionDto>> cancelTransaction(@PathVariable UUID id) {
        StockTransactionDto transaction = stockTransactionService.cancelTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction, "Stock transaction cancelled successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockTransactionDto>> getTransaction(@PathVariable UUID id) {
        StockTransactionDto transaction = stockTransactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StockTransactionDto>>> getTransactions(@PageableDefault(size = 20) Pageable pageable) {
        Page<StockTransactionDto> transactions = stockTransactionService.getTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
