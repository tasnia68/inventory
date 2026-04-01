package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AssignSalaryStructureRequest {
    private UUID employeePayrollProfileId;
    private UUID salaryStructureId;
    private LocalDate effectiveFrom;
    private String notes;
}
