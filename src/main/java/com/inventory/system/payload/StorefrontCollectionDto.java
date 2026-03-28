package com.inventory.system.payload;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontCollectionDto {
    private String id;
    private String slug;
    private String title;
    private String description;
    private Integer sortOrder;
    private String parentId;
    private String parentSlug;
    private Integer level;
    @Builder.Default
    private List<StorefrontCollectionDto> children = new ArrayList<>();
}
