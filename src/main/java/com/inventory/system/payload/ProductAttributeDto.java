package com.inventory.system.payload;

import com.inventory.system.common.entity.ProductAttribute.AttributeType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductAttributeDto {
    private UUID id;
    private String name;
    private AttributeType type;
    private Boolean required;
    private String validationRegex;
    private String options;
    private UUID templateId;
    private UUID groupId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
