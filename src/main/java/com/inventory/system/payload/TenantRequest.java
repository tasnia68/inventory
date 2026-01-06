package com.inventory.system.payload;

import com.inventory.system.common.entity.Tenant.SubscriptionPlan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String subdomain;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    private String adminPassword;

    @NotNull
    private SubscriptionPlan plan;
}
