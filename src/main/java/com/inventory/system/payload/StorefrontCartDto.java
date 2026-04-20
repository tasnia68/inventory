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
public class StorefrontCartDto {
    private String currency;
    private String warehouseId;
    private String warehouseName;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private List<String> appliedCouponCodes = new ArrayList<>();
    private List<StorefrontCartLineDto> lines = new ArrayList<>();
}
