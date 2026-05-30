package com.inventory.system.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Edit-mode counterpart to BulkProductCreateRequest. All fields optional —
 * service applies a diff. Variants identified by id (existing) or absence of
 * id (new); variants present in the DB but missing from the request are NOT
 * deleted by default — pass `deleteMissingVariants=true` to opt in.
 *
 * Stock adjustments are recorded as StockMovement rows via the existing
 * stockService.adjustStock(...) helper, so the audit trail stays intact.
 */
@Data
public class BulkProductUpdateRequest {

    @Valid
    private ProductTemplateDto template;

    @Valid
    private List<BulkProductCreateRequest.AttributeSpec> attributes;

    @Valid
    private List<VariantPatch> variants;

    private Boolean deleteMissingVariants = Boolean.FALSE;

    @Data
    public static class VariantPatch {
        /** Null for newly-added variants; existing variant id for updates. */
        private UUID id;
        private String sku;
        private String barcode;
        private BigDecimal price;
        private BigDecimal compareAtPrice;
        private BigDecimal cost;
        private String storefrontBadge;
        private Boolean storefrontFeatured;
        private Map<String, String> attributeValues;
        /** Each entry creates a StockMovement with the supplied delta. */
        @Valid
        private List<BulkProductCreateRequest.InitialStock> stockAdjustments = new ArrayList<>();
    }

    @NotNull
    public Boolean getDeleteMissingVariants() {
        return deleteMissingVariants == null ? Boolean.FALSE : deleteMissingVariants;
    }
}
