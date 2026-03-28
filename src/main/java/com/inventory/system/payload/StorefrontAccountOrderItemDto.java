package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAccountOrderItemDto {
    private String sku;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal totalPrice;
}
