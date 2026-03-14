package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosShiftTenderCountRequest {

    @NotNull
    private PosPaymentMethod paymentMethod;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal declaredAmount = BigDecimal.ZERO;
}