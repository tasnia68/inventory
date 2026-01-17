package com.inventory.system.payload;

import com.inventory.system.common.entity.StockTransactionStatus;
import com.inventory.system.common.entity.StockTransactionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class StockTransactionDto {
    private UUID id;
    private String transactionNumber;
    private StockTransactionType type;
    private StockTransactionStatus status;
    private UUID sourceWarehouseId;
    private String sourceWarehouseName;
    private UUID destinationWarehouseId;
    private String destinationWarehouseName;
    private String reference;
    private String notes;
    private LocalDateTime transactionDate;
    private List<StockTransactionItemDto> items;
    private LocalDateTime createdAt;
    private String createdBy;
}
