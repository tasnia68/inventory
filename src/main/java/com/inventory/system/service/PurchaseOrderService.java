package com.inventory.system.service;

import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.payload.PurchaseOrderDto;
import com.inventory.system.payload.PurchaseOrderRequest;
import com.inventory.system.payload.PurchaseOrderSearchRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PurchaseOrderService {

    PurchaseOrderDto createPurchaseOrder(PurchaseOrderRequest request);

    PurchaseOrderDto getPurchaseOrderById(UUID id);

    Page<PurchaseOrderDto> getAllPurchaseOrders(PurchaseOrderSearchRequest searchRequest);

    PurchaseOrderDto updatePurchaseOrder(UUID id, PurchaseOrderRequest request);

    PurchaseOrderDto updatePurchaseOrderStatus(UUID id, PurchaseOrderStatus status);

    void deletePurchaseOrder(UUID id);
}
