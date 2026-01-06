package com.inventory.system.payload;

import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Set<String> roles;
}
