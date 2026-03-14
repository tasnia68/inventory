package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosSalePaymentRequest {

    @NotNull
    private PosPaymentMethod paymentMethod;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal amount = BigDecimal.ZERO;

    private String referenceNumber;
    private String notes;
}