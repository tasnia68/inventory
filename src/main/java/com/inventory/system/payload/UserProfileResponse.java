package com.inventory.system.payload;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Set<String> permissions;
    private LocalDateTime createdAt;
}
