package com.inventory.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FinancialEventAutoPostListener {

    private final AccountingService accountingService;
    private final FinancialEventFailureService financialEventFailureService;

    @TransactionalEventListener
    public void onFinancialEventRecorded(FinancialEventRecordedEvent event) {
        try {
            accountingService.postFinancialEvent(event.financialEventId());
        } catch (RuntimeException ex) {
            financialEventFailureService.markFailed(event.financialEventId(), ex.getMessage());
        }
    }
}
