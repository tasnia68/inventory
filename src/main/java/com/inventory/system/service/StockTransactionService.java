package com.inventory.system.service;

import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockTransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockTransactionService {
    StockTransactionDto createTransaction(CreateStockTransactionRequest request);
    StockTransactionDto confirmTransaction(UUID id);
    StockTransactionDto cancelTransaction(UUID id);
    StockTransactionDto getTransaction(UUID id);
    Page<StockTransactionDto> getTransactions(Pageable pageable);
}
