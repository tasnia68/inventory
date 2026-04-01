package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavePayrollDepartmentRequest {
    private String name;
    private String description;
    private Boolean active;
}
