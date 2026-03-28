package com.inventory.system.payload;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class StorefrontLoginVerifyRequest {
    @Email
    private String email;
    private String otpCode;
    private String magicToken;
}
