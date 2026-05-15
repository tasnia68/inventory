package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopifyConnectionDto {
    private String storeDomain;
    private String adminApiToken;
    private String webhookSecret;
    private Boolean enabled;
    private Boolean syncCatalog;
    private Boolean syncOrders;
    private Boolean syncInventory;
    private String health;
    private String lastSyncAt;
    private String lastWebhookAt;
    private Boolean adminApiTokenConfigured;
    private Boolean webhookSecretConfigured;
    private String webhookUrl;
}