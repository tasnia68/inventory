package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAccountProfileDto {
    private String id;
    private String customerId;
    private String email;
    private String name;
    private String address;
    private String emailVerifiedAt;
    private String lastLoginAt;
}
