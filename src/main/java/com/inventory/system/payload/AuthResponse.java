package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tenantId;
    private String tenantSubdomain;
    /** When true, the client must redirect to a change-password screen before using the app. */
    private boolean mustChangePassword;
}
