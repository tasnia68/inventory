package com.inventory.system.payload;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String department;
    private String jobTitle;
    private boolean enabled;
    private boolean forcePasswordChange;
    private Set<String> roles;
    private Set<UUID> warehouseIds;

    /** Present ONLY in the response of admin-create-staff when password was generated server-side. */
    private String generatedPassword;
}
