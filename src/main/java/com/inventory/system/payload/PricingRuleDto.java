package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.PricingRuleAdjustmentType;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.SalesChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PricingRuleDto {
    private UUID id;
    private String name;
    private String code;
    private PricingRuleStatus status;
    private PricingRuleAdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private Integer priority;
    private SalesChannel salesChannel;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private BigDecimal minQuantity;
    private CustomerCategory customerCategory;
    private UUID customerId;
    private String customerName;
    private UUID warehouseId;
    private String warehouseName;
    private UUID terminalId;
    private String terminalName;
    private UUID categoryId;
    private String categoryName;
    private UUID productVariantId;
    private String productVariantSku;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}