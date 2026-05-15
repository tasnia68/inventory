package com.inventory.system.service;

import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.common.entity.PurchaseRequisitionStatus;
import com.inventory.system.payload.ProcurementOverviewDto;
import com.inventory.system.repository.AccountsPayableInvoiceRepository;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.PurchaseOrderRepository;
import com.inventory.system.repository.PurchaseRequisitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementOverviewService {

    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final AccountsPayableInvoiceRepository accountsPayableInvoiceRepository;

    @Transactional(readOnly = true)
    public ProcurementOverviewDto getOverview() {
        long openRequisitions = purchaseRequisitionRepository.findAll().stream()
                .filter(pr -> pr.getStatus() == PurchaseRequisitionStatus.SUBMITTED
                        || pr.getStatus() == PurchaseRequisitionStatus.APPROVED)
                .count();

        List<PurchaseOrder> allPos = purchaseOrderRepository.findAll();
        long openPurchaseOrders = allPos.stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.PENDING
                        || po.getStatus() == PurchaseOrderStatus.APPROVED
                        || po.getStatus() == PurchaseOrderStatus.ISSUED
                        || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .count();

        long pendingReceipts = allPos.stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.ISSUED
                        || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .count();

        // Top suppliers by total PO spend (across all statuses except CANCELLED/REJECTED).
        Map<UUID, List<PurchaseOrder>> bySupplier = allPos.stream()
                .filter(po -> po.getStatus() != PurchaseOrderStatus.CANCELLED
                        && po.getStatus() != PurchaseOrderStatus.REJECTED
                        && po.getSupplier() != null)
                .collect(Collectors.groupingBy(po -> po.getSupplier().getId()));
        List<ProcurementOverviewDto.TopSupplier> topSuppliers = bySupplier.entrySet().stream()
                .map(entry -> {
                    PurchaseOrder first = entry.getValue().get(0);
                    BigDecimal sum = entry.getValue().stream()
                            .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ProcurementOverviewDto.TopSupplier(
                            entry.getKey(),
                            first.getSupplier().getName(),
                            sum,
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(ProcurementOverviewDto.TopSupplier::getTotalSpend).reversed())
                .limit(5)
                .toList();

        // GRNI accrual balance ≈ unpaid received-value:
        //   sum of accepted-qty × PO unit-price for COMPLETED GRNs
        //   minus the total of AP invoice amounts that are linked to a PO and have already drained it
        // Simple proxy: COMPLETED GRN value − total billed against linked POs.
        List<GoodsReceiptNote> grns = goodsReceiptNoteRepository.findAll();
        BigDecimal grnTotal = grns.stream()
                .filter(grn -> grn.getStatus() == GoodsReceiptNoteStatus.COMPLETED
                        || grn.getStatus() == GoodsReceiptNoteStatus.VERIFIED)
                .flatMap(grn -> grn.getItems().stream())
                .map(this::lineValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal billedTotal = accountsPayableInvoiceRepository.findAll().stream()
                .filter(inv -> inv.getPurchaseOrder() != null)
                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grniBalance = grnTotal.subtract(billedTotal).setScale(2, RoundingMode.HALF_UP);

        List<ProcurementOverviewDto.RecentReceipt> recentReceipts = grns.stream()
                .sorted(Comparator.comparing(GoodsReceiptNote::getCreatedAt).reversed())
                .limit(10)
                .map(grn -> new ProcurementOverviewDto.RecentReceipt(
                        grn.getId(),
                        grn.getGrnNumber(),
                        grn.getSupplier() != null ? grn.getSupplier().getName() : null,
                        grn.getStatus() != null ? grn.getStatus().name() : null,
                        grn.getReceivedDate(),
                        grn.getItems().stream().map(this::lineValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        return new ProcurementOverviewDto(
                openRequisitions, openPurchaseOrders, pendingReceipts,
                grniBalance, topSuppliers, recentReceipts
        );
    }

    private BigDecimal lineValue(GoodsReceiptNoteItem item) {
        int qty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
        BigDecimal unit = item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getUnitPrice() != null
                ? item.getPurchaseOrderItem().getUnitPrice() : BigDecimal.ZERO;
        return BigDecimal.valueOf(qty).multiply(unit);
    }
}
