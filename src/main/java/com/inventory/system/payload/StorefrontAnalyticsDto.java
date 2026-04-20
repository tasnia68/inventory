package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAnalyticsDto {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private long newCustomers;
    private List<StatusBreakdown> ordersByStatus;
    private List<TopProduct> topProducts;
    private List<DailyRevenue> dailyRevenue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusBreakdown {
        private String status;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String productName;
        private String sku;
        private BigDecimal totalQuantity;
        private BigDecimal totalRevenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private String date;
        private BigDecimal revenue;
        private long orders;
    }
}
