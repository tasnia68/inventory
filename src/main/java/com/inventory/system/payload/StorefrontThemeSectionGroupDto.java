package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Named group of sections within a template (e.g. "header", "footer", "body").
 * Introduced in theme schema v2; v1 templates have a flat sections list that
 * normalizeThemeDocument() wraps into a single "body" group on read.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeSectionGroupDto {
    private String id;
    private String type;
    private String label;
    private List<StorefrontThemeSectionDto> sections = new ArrayList<>();
}
