package com.inventory.system.payload;

import com.inventory.system.common.entity.Tenant.SubscriptionPlan;
import com.inventory.system.common.entity.Tenant.TenantStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TenantResponse {
    private UUID id;
    private String name;
    private String subdomain;
    private TenantStatus status;
    private SubscriptionPlan subscriptionPlan;
    private LocalDateTime createdAt;
}
