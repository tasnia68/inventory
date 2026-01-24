package com.inventory.system.service;

import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.SalesOrderSearchRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface SalesOrderService {
    SalesOrderDto createSalesOrder(SalesOrderRequest request);
    SalesOrderDto getSalesOrderById(UUID id);
    Page<SalesOrderDto> getAllSalesOrders(SalesOrderSearchRequest request);
    SalesOrderDto updateSalesOrder(UUID id, SalesOrderRequest request);
    SalesOrderDto updateSalesOrderStatus(UUID id, SalesOrderStatus status);
    void cancelSalesOrder(UUID id);
}
