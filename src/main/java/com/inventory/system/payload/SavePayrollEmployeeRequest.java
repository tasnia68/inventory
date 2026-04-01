package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class SavePayrollEmployeeRequest {
    private UUID userId;
    private String employeeCode;
    private UUID departmentId;
    private UUID designationId;
    private PayrollPayFrequency payFrequency;
    private LocalDate joinDate;
    private Boolean active;
    private String notes;
}
