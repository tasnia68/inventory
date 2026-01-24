package com.inventory.system.payload;

import lombok.Data;
import java.util.Set;

@Data
public class CreateRoleRequest {
    private String name;
    private String description;
    private Set<String> permissions;
}
