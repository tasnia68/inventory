package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontDomainDto {
    private UUID id;
    private String hostname;
    private boolean primary;
    private boolean active;
    private String verificationStatus;
    private String tlsStatus;
    private LocalDateTime verificationCheckedAt;
    private LocalDateTime activatedAt;
    private String lastError;
}
