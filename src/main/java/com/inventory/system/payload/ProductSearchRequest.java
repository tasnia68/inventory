package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductSearchRequest {
    private String q;
    private UUID categoryId;
    private UUID templateId;
    private UUID attributeId;
    private String attributeValue;
}