package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductVariantDto {
    private UUID id;
    private String sku;
    private String barcode;
    private BigDecimal price;
    private UUID templateId;
    private UUID mainImageId;
    private String mainImageUrl;
    private List<AttributeValueDto> attributeValues;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @Data
    public static class AttributeValueDto {
        private UUID attributeId;
        private String value;
    }
}
