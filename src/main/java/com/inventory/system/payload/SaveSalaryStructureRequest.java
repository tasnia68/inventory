package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SaveSalaryStructureRequest {
    private String name;
    private String description;
    private PayrollPayFrequency payFrequency;
    private Boolean active;
    private List<ComponentInput> components = new ArrayList<>();

    @Getter
    @Setter
    public static class ComponentInput {
        private UUID payrollComponentId;
        private BigDecimal amount;
        private BigDecimal rate;
        private Integer sortOrder;
    }
}
