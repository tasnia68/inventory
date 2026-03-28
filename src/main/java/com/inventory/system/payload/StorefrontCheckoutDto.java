package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontCheckoutDto {
    private String checkoutStatus;
    private String message;
    private SalesOrderDto order;
}
