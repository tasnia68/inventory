package com.inventory.system.payload;

import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.PromotionDiscountType;
import com.inventory.system.common.entity.PromotionScope;
import com.inventory.system.common.entity.PromotionStatus;
import com.inventory.system.common.entity.SalesChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreatePromotionRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private String description;

    @NotNull
    private PromotionStatus status;

    @NotNull
    private PromotionDiscountType discountType;

    @NotNull
    private PromotionScope scope;

    private SalesChannel salesChannel;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean stackable;
    private Boolean couponRequired;
    private Integer priority;
    private String exclusionGroup;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal minQuantity;
    private BigDecimal bundleQuantity;
    private BigDecimal bundlePrice;
    private BigDecimal buyQuantity;
    private BigDecimal getQuantity;
    private Integer usageLimitTotal;
    private Integer usageLimitPerCustomer;
    private CustomerCategory customerCategory;
    private UUID warehouseId;
    private UUID terminalId;
    private UUID categoryId;
    private UUID productVariantId;
}