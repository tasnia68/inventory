package com.inventory.system.payload;

import com.inventory.system.common.entity.ProductAttribute.AttributeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One-shot product creation: template + attributes + variants + initial stock.
 *
 * Replaces the 4+ round trip wizard flow with a single transactional POST.
 * Images are NOT included here — they're uploaded as multipart to
 * /api/v1/product-templates/{templateId}/images. The frontend creates the
 * product first, then attaches images on the returned templateId.
 */
@Data
public class BulkProductCreateRequest {

    @Valid
    @NotNull(message = "Template payload is required")
    private ProductTemplateDto template;

    @Valid
    private List<AttributeSpec> attributes = new ArrayList<>();

    @Valid
    @NotNull(message = "At least one variant is required")
    private List<VariantSpec> variants = new ArrayList<>();

    @Data
    public static class AttributeSpec {
        @NotBlank(message = "Attribute name is required")
        private String name;
        private AttributeType type = AttributeType.DROPDOWN;
        private Boolean required = Boolean.FALSE;
        private String options; // comma-separated values for DROPDOWN/MULTI_SELECT
        private UUID groupId;
    }

    @Data
    public static class VariantSpec {
        /** Optional — if blank, the backend generates one via the existing generateSku() helper. */
        private String sku;
        private String barcode;
        @NotNull(message = "Variant price is required")
        private BigDecimal price;
        private BigDecimal compareAtPrice;
        private BigDecimal cost;
        private String storefrontBadge;
        private Boolean storefrontFeatured = Boolean.FALSE;
        /** Keyed by attribute name (matches AttributeSpec.name); value is the chosen option text. */
        private Map<String, String> attributeValues;
        @Valid
        private List<InitialStock> initialStocks = new ArrayList<>();
    }

    @Data
    public static class InitialStock {
        @NotNull(message = "Warehouse ID is required for initial stock")
        private UUID warehouseId;
        @NotNull(message = "Quantity is required for initial stock")
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private UUID batchId;
        private String reason; // optional note recorded against the stock movement
    }
}
