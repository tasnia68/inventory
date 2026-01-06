package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenant_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"setting_key", "tenant_id"})
})
@Getter
@Setter
public class TenantSetting extends BaseEntity {

    @NotBlank
    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @NotBlank
    @Column(name = "setting_type", nullable = false)
    private String settingType; // STRING, BOOLEAN, NUMBER, JSON

    @Column(name = "category")
    private String category; // e.g., GENERAL, BRANDING, NOTIFICATION
}
