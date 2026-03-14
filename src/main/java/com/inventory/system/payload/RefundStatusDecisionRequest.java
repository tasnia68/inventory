package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class RefundStatusDecisionRequest {
    private String notes;
    private UUID shiftId;
    private UUID terminalId;
}