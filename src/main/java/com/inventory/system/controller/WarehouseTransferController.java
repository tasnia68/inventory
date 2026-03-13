package com.inventory.system.controller;

import com.inventory.system.common.entity.StockTransactionType;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.payload.WarehouseTransferRequest;
import com.inventory.system.service.StockTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/warehouse-transfers")
@RequiredArgsConstructor
public class WarehouseTransferController {

    private final StockTransactionService stockTransactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransactionDto>> transfer(@Valid @RequestBody WarehouseTransferRequest request) {
        CreateStockTransactionRequest txRequest = new CreateStockTransactionRequest();
        txRequest.setType(StockTransactionType.TRANSFER);
        txRequest.setSourceWarehouseId(request.getSourceWarehouseId());
        txRequest.setDestinationWarehouseId(request.getDestinationWarehouseId());
        txRequest.setReference(request.getReference());
        txRequest.setNotes(request.getNotes());
        txRequest.setItems(request.getItems().stream().map(item -> {
            CreateStockTransactionRequest.ItemRequest ir = new CreateStockTransactionRequest.ItemRequest();
            ir.setProductVariantId(item.getProductVariantId());
            ir.setQuantity(item.getQuantity());
            ir.setUnitCost(item.getUnitCost());
            ir.setSourceStorageLocationId(item.getSourceStorageLocationId());
            ir.setDestinationStorageLocationId(item.getDestinationStorageLocationId());
            return ir;
        }).collect(Collectors.toList()));

        StockTransactionDto created = stockTransactionService.createTransaction(txRequest);
        StockTransactionDto confirmed = stockTransactionService.confirmTransaction(created.getId());
        return ResponseEntity.ok(ApiResponse.success(confirmed, "Warehouse transfer completed successfully"));
    }
}
