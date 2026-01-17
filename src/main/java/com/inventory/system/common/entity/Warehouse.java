package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
public class Warehouse extends BaseEntity {

    @NotBlank(message = "Warehouse name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String location;

    @NotBlank(message = "Warehouse type is required")
    @Column(nullable = false)
    private String type;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
