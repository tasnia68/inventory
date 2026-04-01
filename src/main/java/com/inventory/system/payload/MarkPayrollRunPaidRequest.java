package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MarkPayrollRunPaidRequest {
    private LocalDate paymentDate;
    private String reference;
    private String notes;
}
