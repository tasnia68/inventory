package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenant_virtual_try_on_settings")
@Getter
@Setter
public class TenantVirtualTryOnSettings extends AuditableEntity {

    @Id
    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "max_per_customer_per_day", nullable = false)
    private int maxPerCustomerPerDay = 3;

    @Column(name = "max_per_tenant_per_month", nullable = false)
    private int maxPerTenantPerMonth = 500;
}
