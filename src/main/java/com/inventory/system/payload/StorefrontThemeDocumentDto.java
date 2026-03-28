package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeDocumentDto {
    private String templateKey;
    private String schemaVersion;
    private Map<String, Object> settings = new LinkedHashMap<>();
    private Map<String, StorefrontThemeTemplateDto> templates = new LinkedHashMap<>();
}
