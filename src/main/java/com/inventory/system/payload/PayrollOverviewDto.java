package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PayrollOverviewDto {
    private long employeeCount;
    private long departmentCount;
    private long structureCount;
    private long draftRuns;
    private long approvedRuns;
    private long paidRuns;
    private BigDecimal totalApprovedNetPay = BigDecimal.ZERO;
    private List<PayrollRunDto> recentRuns = new ArrayList<>();
}
