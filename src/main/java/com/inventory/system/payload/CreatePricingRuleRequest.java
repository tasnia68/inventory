package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.PricingRuleAdjustmentType;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.SalesChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreatePricingRuleRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotNull
    private PricingRuleStatus status;

    @NotNull
    private PricingRuleAdjustmentType adjustmentType;

    @NotNull
    private BigDecimal adjustmentValue;

    private Integer priority;
    private SalesChannel salesChannel;

    @NotNull
    private LocalDateTime validFrom;

    private LocalDateTime validTo;
    private BigDecimal minQuantity;
    private CustomerCategory customerCategory;
    private UUID customerId;
    private UUID warehouseId;
    private UUID terminalId;
    private UUID categoryId;
    private UUID productVariantId;
    private String notes;
}