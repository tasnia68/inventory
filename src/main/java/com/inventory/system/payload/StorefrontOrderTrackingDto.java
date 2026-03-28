package com.inventory.system.payload;

import com.inventory.system.common.entity.SalesOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontOrderTrackingDto {
    private String orderNumber;
    private SalesOrderStatus status;
    private String customerName;
    private String customerEmail;
    private String customerPhoneNumber;
    private String warehouseName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private String currency;
    private List<SalesOrderItemDto> items = new ArrayList<>();
}
