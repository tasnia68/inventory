package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SalaryStructureDto {
    private UUID id;
    private String name;
    private String description;
    private PayrollPayFrequency payFrequency;
    private boolean active;
    private List<SalaryStructureComponentDto> components = new ArrayList<>();
}
