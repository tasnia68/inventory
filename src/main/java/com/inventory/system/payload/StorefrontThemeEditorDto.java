package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeEditorDto {
    private String schemaVersion;
    private String activeRevisionId;
    private StorefrontThemeDocumentDto draftThemeDocument;
    private StorefrontThemeDocumentDto publishedThemeDocument;
    private Map<String, Object> schema = new LinkedHashMap<>();
    private List<StorefrontPublishVersionDto> revisions;
}
