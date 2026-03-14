package com.inventory.system.payload;

import com.inventory.system.common.entity.CouponStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CouponDto {
    private UUID id;
    private UUID promotionId;
    private String promotionName;
    private String promotionCode;
    private String code;
    private CouponStatus status;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer maxRedemptionsTotal;
    private Integer maxRedemptionsPerCustomer;
    private Integer redeemedCount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}