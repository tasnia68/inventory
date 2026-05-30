package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductTemplateDto {
    private UUID id;
    private String name;
    private String description;
    private Boolean isActive;
    private String status; // DRAFT | ACTIVE | ARCHIVED
    private String vendor;
    private String productType;
    private String tags; // comma-separated free tags
    private UUID categoryId;
    private String categoryName;
    private UUID uomId;
    private String uomName;
    private Boolean publishedToStorefront;
    private String storefrontSlug;
    private String storefrontTitle;
    private String storefrontDescription;
    private Integer storefrontSortOrder;
    private String storefrontSeoTitle;
    private String storefrontSeoDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
