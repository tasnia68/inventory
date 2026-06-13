package com.inventory.system.accounting.api.event;

public interface FinancialEventSource {
    com.inventory.system.payload.FinancialEventDto recordFinancialEvent(FinancialEventDto event);
}