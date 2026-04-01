package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreatePayrollRunRequest {
    private String title;
    private PayrollPayFrequency payFrequency;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String currency;
    private String notes;
    private List<UUID> employeePayrollProfileIds = new ArrayList<>();
}
