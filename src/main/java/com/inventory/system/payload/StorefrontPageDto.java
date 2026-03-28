package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontPageDto {
    private String slug;
    private String title;
    private List<StorefrontSectionDto> sections = new ArrayList<>();
}

