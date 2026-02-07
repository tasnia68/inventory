package com.inventory.system.payload;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductImageDto {
    private UUID id;
    private String url;
    private String filename;
    private Boolean isMain;
    private UUID productTemplateId;
}
