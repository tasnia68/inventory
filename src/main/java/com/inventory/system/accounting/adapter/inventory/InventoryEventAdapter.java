package com.inventory.system.accounting.adapter.inventory;

import com.inventory.system.accounting.api.event.FinancialEventDto;
import com.inventory.system.accounting.api.event.FinancialEventSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventAdapter {
    private final FinancialEventSource financialEventSource;

    public com.inventory.system.payload.FinancialEventDto record(FinancialEventDto event) {
        return financialEventSource.recordFinancialEvent(event);
    }
}