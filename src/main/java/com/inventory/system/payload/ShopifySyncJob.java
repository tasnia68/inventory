package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Message body for an async Shopify sync job consumed by the RabbitMQ worker. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopifySyncJob implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;
    private String runId;
    private String syncType;
}
