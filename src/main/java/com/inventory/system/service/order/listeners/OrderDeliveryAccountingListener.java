package com.inventory.system.service.order.listeners;

import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.CreateAccountsReceivableInvoiceRequest;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.AccountingService;
import com.inventory.system.service.FinancialEventService;
import com.inventory.system.service.TenantSettingService;
import com.inventory.system.service.order.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeliveryAccountingListener {

    private static final String AUTO_POST_ON_DELIVERED_KEY = "accounting.postOnDelivered";
    private static final String AUTO_POST_ON_PARTIAL_KEY = "accounting.postPartialOnPartialDelivered";

    private final SalesOrderRepository salesOrderRepository;
    private final AccountingService accountingService;
    private final FinancialEventService financialEventService;
    private final TenantSettingService tenantSettingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        SalesOrderStatus target = event.toStatus();
        boolean partial = target == SalesOrderStatus.PARTIALLY_DELIVERED;
        boolean full = target == SalesOrderStatus.DELIVERED;
        boolean returned = target == SalesOrderStatus.RETURNED;
        boolean partialCancel = target == SalesOrderStatus.PARTIALLY_CANCELLED;

        if (!full && !partial && !returned && !partialCancel) return;

        SalesOrder salesOrder = salesOrderRepository.findById(event.salesOrderId()).orElse(null);
        if (salesOrder == null) {
            log.warn("Order {} not found when posting delivery accounting", event.salesOrderId());
            return;
        }

        if (returned) {
            try {
                financialEventService.recordSalesOrderReturn(salesOrder);
            } catch (Exception e) {
                log.error("Failed to post return reversal for order {}: {}", salesOrder.getSoNumber(), e.getMessage(), e);
            }
            return;
        }

        if (partialCancel) {
            try {
                financialEventService.recordSalesOrderPartialCancel(salesOrder);
                financialEventService.recordCourierFee(salesOrder, false);
                financialEventService.recordCodCollectionFee(salesOrder, false);
            } catch (Exception e) {
                log.error("Failed to post partial-cancel accounting for order {}: {}", salesOrder.getSoNumber(), e.getMessage(), e);
            }
            return;
        }

        String flagKey = partial ? AUTO_POST_ON_PARTIAL_KEY : AUTO_POST_ON_DELIVERED_KEY;
        if (!autoPostEnabled(flagKey)) {
            return;
        }

        try {
            BigDecimal invoiceAmount = partial
                    ? partialInvoiceAmount(salesOrder)
                    : salesOrder.getTotalAmount();
            if (invoiceAmount != null && invoiceAmount.compareTo(BigDecimal.ZERO) > 0) {
                CreateAccountsReceivableInvoiceRequest invoiceRequest = new CreateAccountsReceivableInvoiceRequest();
                invoiceRequest.setCustomerId(salesOrder.getCustomer().getId());
                invoiceRequest.setSalesOrderId(salesOrder.getId());
                invoiceRequest.setCustomerInvoiceNumber("INV-" + salesOrder.getSoNumber());
                invoiceRequest.setInvoiceDate(LocalDate.now());
                invoiceRequest.setDueDate(LocalDate.now());
                invoiceRequest.setCurrency(salesOrder.getCurrency());
                invoiceRequest.setTotalAmount(invoiceAmount);
                invoiceRequest.setNotes(partial
                        ? "Auto-generated invoice for partial delivery of " + salesOrder.getSoNumber()
                        : "Auto-generated invoice on delivery of " + salesOrder.getSoNumber());
                accountingService.createAccountsReceivableInvoice(invoiceRequest);
            }
            financialEventService.recordSalesOrderDelivery(salesOrder, partial);
            financialEventService.recordCourierFee(salesOrder, partial);
            financialEventService.recordCodCollectionFee(salesOrder, partial);
        } catch (Exception e) {
            log.error("Failed to post delivery accounting for order {}: {}", salesOrder.getSoNumber(), e.getMessage(), e);
        }
    }

    private BigDecimal partialInvoiceAmount(SalesOrder salesOrder) {
        BigDecimal shipping = salesOrder.getShippingAmount() != null ? salesOrder.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal linesTotal = salesOrder.getItems().stream()
                .map(item -> {
                    BigDecimal fulfilled = item.getFulfilledQuantity() != null ? item.getFulfilledQuantity() : BigDecimal.ZERO;
                    BigDecimal unit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    return fulfilled.multiply(unit);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return linesTotal.add(shipping);
    }

    private boolean autoPostEnabled(String key) {
        return tenantSettingService.findSetting(key)
                .map(s -> s.getValue())
                .map(v -> !"false".equalsIgnoreCase(v.trim()))
                .orElse(true);
    }
}
