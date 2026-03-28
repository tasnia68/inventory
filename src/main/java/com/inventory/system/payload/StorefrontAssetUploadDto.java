package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAssetUploadDto {
    private String assetType;
    private String filename;
    private String storagePath;
    private String publicUrl;
}
