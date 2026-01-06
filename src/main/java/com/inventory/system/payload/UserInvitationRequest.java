package com.inventory.system.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserInvitationRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String roleName;
}
