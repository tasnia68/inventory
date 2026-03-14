package com.inventory.system.payload;

import com.inventory.system.common.entity.CouponStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCouponRequest {
    @NotBlank
    private String code;

    @NotNull
    private CouponStatus status;

    @NotNull
    private LocalDateTime validFrom;

    private LocalDateTime validTo;
    private Integer maxRedemptionsTotal;
    private Integer maxRedemptionsPerCustomer;
    private String notes;
}