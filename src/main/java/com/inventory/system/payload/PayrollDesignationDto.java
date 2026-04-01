package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PayrollDesignationDto {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
}
