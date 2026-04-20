package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontCmsPageDto {
    private UUID id;
    private String title;
    private String slug;
    private String body;
    private boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
