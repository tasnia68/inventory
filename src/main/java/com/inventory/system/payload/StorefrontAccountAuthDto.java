package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAccountAuthDto {
    private String sessionToken;
    private String expiresAt;
    private StorefrontAccountProfileDto profile;
}
