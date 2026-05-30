package com.inventory.system.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * What the admin Themes page needs to render the "update available" banner:
 * which theme the tenant is on, what version their latest publish was authored
 * against, what version the bundled manifest is on, and whether to nudge.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorefrontThemeUpgradeStatusDto {
    private String themeKey;
    private String themeName;
    private String activeVersion;
    private String availableVersion;
    private boolean hasUpgrade;
    private String activeVersionPublishedAt;
    private List<Map<String, Object>> migrations;
}
