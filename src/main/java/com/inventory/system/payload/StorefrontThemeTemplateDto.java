package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeTemplateDto {
    private String id;
    private String label;
    private List<StorefrontThemeSectionDto> sections = new ArrayList<>();
}
