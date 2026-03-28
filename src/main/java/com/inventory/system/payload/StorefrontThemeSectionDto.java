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
public class StorefrontThemeSectionDto {
    private String id;
    private String type;
    private String label;
    private String variant;
    private Boolean enabled;
    private Map<String, Object> settings = new LinkedHashMap<>();
    private List<StorefrontThemeBlockDto> blocks = new ArrayList<>();
}
