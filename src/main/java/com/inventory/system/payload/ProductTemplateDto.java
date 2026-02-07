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
    private UUID categoryId;
    private String categoryName;
    private UUID uomId;
    private String uomName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
