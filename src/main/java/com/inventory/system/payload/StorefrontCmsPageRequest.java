package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StorefrontCmsPageRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String slug;
    private String body;
    private boolean published;
}
