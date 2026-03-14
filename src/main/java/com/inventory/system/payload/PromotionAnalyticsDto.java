package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PromotionAnalyticsDto {
    private long appliedCount;
    private long flaggedCount;
    private BigDecimal totalDiscount;
    private List<PromotionUsageSummaryDto> promotions = new ArrayList<>();

    @Data
    public static class PromotionUsageSummaryDto {
        private String promotionCode;
        private String promotionName;
        private long appliedCount;
        private long flaggedCount;
        private BigDecimal totalDiscount;
    }
}