package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class CategoryPermissionDto {
    private UUID id;
    private UUID categoryId;
    private UUID roleId;
    private String roleName;
    private Boolean canView;
    private Boolean canEdit;
}