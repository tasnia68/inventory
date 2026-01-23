package com.inventory.system.payload;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class PurchaseOrderSearchRequest {

    private UUID supplierId;
    private PurchaseOrderStatus status;
    private String poNumber;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "orderDate";
    private String sortDirection = "desc";
}
