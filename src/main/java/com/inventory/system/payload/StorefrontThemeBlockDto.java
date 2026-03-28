package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeBlockDto {
    private String id;
    private String type;
    private String label;
    private Boolean enabled;
    private Map<String, Object> settings = new LinkedHashMap<>();
}
