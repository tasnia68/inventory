package com.inventory.system.accounting.adapter.inventory;

import com.inventory.system.accounting.api.event.AccountingSourceDocumentPort;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.ReconciliationSourceType;
import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.PosShiftRepository;
import com.inventory.system.repository.PurchaseOrderRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryAccountingSourceDocumentAdapter implements AccountingSourceDocumentPort {
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final PosShiftRepository posShiftRepository;

    @Override
    public PayableSource resolvePayableSource(UUID supplierId, UUID purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderId == null ? null : purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", purchaseOrderId));
        Supplier supplier = supplierId != null
                ? supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", supplierId))
                : purchaseOrder.getSupplier();
        if (purchaseOrder != null && !purchaseOrder.getSupplier().getId().equals(supplier.getId())) {
            throw new BadRequestException("Purchase order supplier does not match the selected supplier");
        }
        return new PayableSource(
            supplier.getId(),
            supplier.getName(),
            purchaseOrder == null ? null : purchaseOrder.getId(),
            purchaseOrder == null ? null : purchaseOrder.getPoNumber(),
                purchaseOrder == null ? null : purchaseOrder.getTotalAmount(),
                purchaseOrder == null ? null : purchaseOrder.getCurrency()
        );
    }

    @Override
    public ReceivableSource resolveReceivableSource(UUID customerId, UUID salesOrderId) {
        var salesOrder = salesOrderId == null ? null : salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", salesOrderId));
        Customer customer = customerId != null
                ? customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId))
                : salesOrder.getCustomer();
        if (salesOrder != null && !salesOrder.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Sales order customer does not match the selected customer");
        }
        return new ReceivableSource(
            customer.getId(),
            customer.getName(),
            salesOrder == null ? null : salesOrder.getId(),
            salesOrder == null ? null : salesOrder.getSoNumber(),
                salesOrder == null ? null : salesOrder.getTotalAmount(),
                salesOrder == null ? null : salesOrder.getCurrency()
        );
    }

    @Override
    public String assertThreeWayMatch(UUID purchaseOrderId, BigDecimal invoiceTotal, boolean force) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", purchaseOrderId));
        List<GoodsReceiptNote> grns = goodsReceiptNoteRepository.findByPurchaseOrderId(purchaseOrder.getId());
        if (grns.isEmpty()) {
            if (force) {
                return "3-way match override: no GRN exists for PO " + purchaseOrder.getPoNumber() + " (force=true)";
            }
            throw new BadRequestException("Cannot post invoice - no goods receipt has been recorded against PO "
                    + purchaseOrder.getPoNumber() + " yet. Create a GRN first, or set force=true to override.");
        }

        BigDecimal receivedValue = grns.stream()
                .flatMap(grn -> grn.getItems().stream())
                .map(item -> {
                    int qty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
                    BigDecimal unit = item.getPurchaseOrderItem() != null && item.getPurchaseOrderItem().getUnitPrice() != null
                            ? item.getPurchaseOrderItem().getUnitPrice() : BigDecimal.ZERO;
                    return BigDecimal.valueOf(qty).multiply(unit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal poTotal = purchaseOrder.getTotalAmount() != null ? scale(purchaseOrder.getTotalAmount()) : BigDecimal.ZERO;
        BigDecimal expected = receivedValue.compareTo(poTotal) < 0 ? receivedValue : poTotal;
        if (expected.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal variance = scale(invoiceTotal).subtract(expected).abs();
        BigDecimal tolerance = expected.multiply(new BigDecimal("0.02"));
        if (variance.compareTo(tolerance) > 0) {
            String detail = String.format(
                    "PO total %s, received value %s, invoice total %s, variance %s (tolerance 2%% of expected %s)",
                    poTotal, receivedValue, invoiceTotal, variance, expected);
            if (force) {
                return "3-way match override: " + detail;
            }
            throw new BadRequestException("Three-way match failed for PO " + purchaseOrder.getPoNumber()
                    + ". " + detail + ". Set force=true to override.");
        }
        return null;
    }

    @Override
    public List<CashReconciliationSeed> findCashReconciliationSeeds(LocalDate businessDate) {
        return posShiftRepository.findAll().stream()
                .filter(shift -> shift.getClosedAt() != null && businessDate.equals(shift.getClosedAt().toLocalDate()))
                .filter(shift -> shift.getDeclaredCashAmount() != null && shift.getDeclaredCashAmount().compareTo(BigDecimal.ZERO) != 0)
                .map(shift -> new CashReconciliationSeed(
                        ReconciliationSourceType.POS_SHIFT,
                        shift.getId().toString(),
                        shift.getTerminal().getTerminalCode(),
                        shift.getClosedAt().toLocalDate(),
                        "POS shift cash settlement for " + shift.getTerminal().getName(),
                        scale(shift.getDeclaredCashAmount())
                ))
                .toList();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : value.setScale(6, RoundingMode.HALF_UP);
    }
}