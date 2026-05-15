package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementOverviewDto {
    private long openRequisitions;
    private long openPurchaseOrders;
    private long pendingReceipts;
    private BigDecimal grniAccrualBalance;
    private List<TopSupplier> topSuppliers;
    private List<RecentReceipt> recentReceipts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSupplier {
        private UUID supplierId;
        private String supplierName;
        private BigDecimal totalSpend;
        private long orderCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReceipt {
        private UUID id;
        private String grnNumber;
        private String supplierName;
        private String status;
        private LocalDateTime receivedAt;
        private BigDecimal value;
    }
}
