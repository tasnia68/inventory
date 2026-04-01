package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PayrollEmployeeDto {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String employeeCode;
    private UUID departmentId;
    private String departmentName;
    private UUID designationId;
    private String designationName;
    private PayrollPayFrequency payFrequency;
    private LocalDate joinDate;
    private boolean active;
    private String notes;
    private UUID activeSalaryStructureId;
    private String activeSalaryStructureName;
}
