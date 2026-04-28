package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourierProfileDto {
    private UUID id;
    private String providerCode;
    private String displayName;
    private boolean isDefault;
    private boolean isActive;
    private String credentialsJson;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
