package com.inventory.system.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDto {
    private UUID id;
    private String name;
    private String description;
    private UUID parentId;
    private List<CategoryDto> children = new ArrayList<>();
    private Set<UUID> attributeIds = new HashSet<>();
}
