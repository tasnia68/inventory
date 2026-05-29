package com.inventory.system.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;

    /** Optional. If blank and {@link #generatePassword} is true, server generates a 10-char temp password. */
    private String password;

    /** When true, server generates a random temp password regardless of {@link #password}. */
    private boolean generatePassword = false;

    private String firstName;
    private String lastName;
    private String phone;
    private String department;
    private String jobTitle;

    private Set<String> roles;
    private Set<UUID> warehouseIds;

    /**
     * When true, the user is forced to change their password on first login.
     * Defaults to true for admin-created staff (they're given a temp password).
     */
    private boolean forcePasswordChange = true;
}
