package com.inventory.system.payload;

import com.inventory.system.common.entity.PosPaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePosSaleRequest {
    private String clientSaleId;

    @NotNull
    private UUID terminalId;

    private UUID customerId;

    private UUID shiftId;

    @NotNull
    private UUID warehouseId;

    @NotNull
    private PosPaymentMethod paymentMethod;

    @DecimalMin(value = "0.0")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    private BigDecimal tenderedAmount = BigDecimal.ZERO;

    private String currency;
    private String notes;

    private List<String> couponCodes = new ArrayList<>();

    @Valid
    @NotEmpty
    private List<PosSaleItemRequest> items;
}