package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PosSalePaymentDto {
    private UUID id;
    private PosPaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNumber;
    private String notes;
}