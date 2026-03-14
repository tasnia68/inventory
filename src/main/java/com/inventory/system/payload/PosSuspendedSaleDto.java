package com.inventory.system.payload;

import com.inventory.system.common.entity.PosSuspendedSaleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PosSuspendedSaleDto {
    private UUID id;
    private String suspendedNumber;
    private UUID terminalId;
    private String terminalName;
    private UUID cashierId;
    private String cashierName;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private PosSuspendedSaleStatus status;
    private LocalDateTime suspendedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private BigDecimal manualDiscountAmount;
    private BigDecimal taxAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal totalAmount;
    private String currency;
    private List<String> couponCodes;
    private String notes;
    private List<SuspendedPosSaleItemDto> items;
}