package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontThemeDto {
    private String primary;
    private String secondary;
    private String accent;
    private String surface;
    private String text;
    private Integer radius;
    private String heroAlignment;
    private String headingFont;
    private String bodyFont;
}
