package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AttributeGroupDto {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
