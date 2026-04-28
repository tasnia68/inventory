package com.inventory.system.service;

import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.ConfirmOrderRequest;
import com.inventory.system.payload.PartialDeliveryLineRequest;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.SalesOrderSearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SalesOrderService {
    SalesOrderDto createSalesOrder(SalesOrderRequest request);
    SalesOrderDto getSalesOrderById(UUID id);
    Page<SalesOrderDto> getAllSalesOrders(SalesOrderSearchRequest request);
    SalesOrderDto updateSalesOrder(UUID id, SalesOrderRequest request);
    SalesOrderDto updateSalesOrderStatus(UUID id, SalesOrderStatus status);
    void cancelSalesOrder(UUID id);

    Set<SalesOrderStatus> getAllowedTransitions(UUID id);
    SalesOrderDto holdOrder(UUID id, String reason);
    SalesOrderDto confirmOrder(UUID id, ConfirmOrderRequest request);
    SalesOrderDto partialDeliver(UUID id, List<PartialDeliveryLineRequest> lines);
    SalesOrderDto updateItems(UUID id, List<com.inventory.system.payload.SalesOrderItemRequest> items);
}
