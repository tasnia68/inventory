package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontSectionDto {
    private String id;
    private String label;
    private String type;
    private boolean enabled;
    private String variant;
    private Map<String, Object> config = new LinkedHashMap<>();
    private List<StorefrontThemeBlockDto> blocks = new ArrayList<>();

    public StorefrontSectionDto(String id, String label, String type, boolean enabled, String variant, Map<String, Object> config) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.enabled = enabled;
        this.variant = variant;
        this.config = config != null ? config : new LinkedHashMap<>();
        this.blocks = new ArrayList<>();
    }
}
