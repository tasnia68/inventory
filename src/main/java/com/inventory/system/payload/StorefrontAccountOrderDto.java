package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontAccountOrderDto {
    private String orderNumber;
    private String status;
    private String orderDate;
    private BigDecimal totalAmount;
    private String currency;
    private List<StorefrontAccountOrderItemDto> items = new ArrayList<>();
}
