package com.inventory.system.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorefrontThemeManifestDto {
    private String key;
    private String name;
    private String version;
    private String description;
    private String author;
    private String screenshot;
    private List<String> tags;
    private List<String> supportedSectionGroups;
    private Map<String, Object> defaultSettings = new LinkedHashMap<>();
    private List<Map<String, Object>> templateTree;
    private List<Map<String, Object>> settingsGroups;
    private Map<String, Object> sectionDefinitions = new LinkedHashMap<>();
    private Map<String, Object> blockDefinitions = new LinkedHashMap<>();
    private Map<String, Object> templatePresets = new LinkedHashMap<>();
    private List<Map<String, Object>> migrations;
}
