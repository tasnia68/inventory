package com.inventory.system.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PayrollUserOptionDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
}
