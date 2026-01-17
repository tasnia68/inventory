package com.inventory.system.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class ProductSearchDto {
    private UUID templateId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sku;
    // Map of AttributeName -> AttributeValue (as string)
    private Map<String, String> attributes;
}
