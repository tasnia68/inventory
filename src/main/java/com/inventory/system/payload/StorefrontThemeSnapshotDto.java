package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeSnapshotDto {
    private String schemaVersion;
    private String activeRevisionId;
    private StorefrontThemeDocumentDto themeDocument;
    private StorefrontConfigDto config;
}
