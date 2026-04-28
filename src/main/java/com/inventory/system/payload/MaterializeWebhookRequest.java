package com.inventory.system.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class MaterializeWebhookRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID warehouseId;

    /**
     * When true and {@link #items} is empty, the service will parse the webhook payload and
     * attempt to map line items to ProductVariants by SKU. Fails fast if any SKU cannot be
     * resolved, so the operator can fix mappings before retrying.
     */
    private boolean autoMapItemsBySku = true;

    /**
     * Optional explicit item overrides. When non-empty, used verbatim and the auto-mapping
     * step is skipped.
     */
    private List<SalesOrderItemRequest> items = new ArrayList<>();

    private String currency;
    private String notes;
}
