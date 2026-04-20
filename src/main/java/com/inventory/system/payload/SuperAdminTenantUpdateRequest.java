package com.inventory.system.payload;

import com.inventory.system.common.entity.Tenant.SubscriptionPlan;
import com.inventory.system.common.entity.Tenant.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuperAdminTenantUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String subdomain;

    @NotNull
    private SubscriptionPlan plan;

    @NotNull
    private TenantStatus status;
}
