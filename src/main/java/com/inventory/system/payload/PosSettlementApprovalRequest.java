package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PosSettlementApprovalRequest {

    @NotNull
    private Boolean approved;

    private String notes;
}