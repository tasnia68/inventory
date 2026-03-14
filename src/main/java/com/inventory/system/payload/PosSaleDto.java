package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import com.inventory.system.common.entity.PosSaleStatus;
import com.inventory.system.common.entity.PosSyncStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PosSaleDto {
    private UUID id;
    private String receiptNumber;
    private String clientSaleId;
    private UUID terminalId;
    private String terminalName;
    private UUID shiftId;
    private UUID cashierId;
    private String cashierName;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private UUID salesOrderId;
    private String soNumber;
    private UUID stockTransactionId;
    private PosSaleStatus saleStatus;
    private PosSyncStatus syncStatus;
    private PosPaymentMethod paymentMethod;
    private LocalDateTime saleTime;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal tenderedAmount;
    private BigDecimal changeAmount;
    private String currency;
    private String notes;
    private List<PosSaleItemDto> items;
}