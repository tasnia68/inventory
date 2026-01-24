package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.StockReservationRequest;
import com.inventory.system.repository.*;
import com.inventory.system.service.impl.SalesOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private StockReservationService stockReservationService;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private SalesOrderServiceImpl salesOrderService;

    private Customer customer;
    private Warehouse warehouse;
    private ProductVariant productVariant;
    private SalesOrderRequest request;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test Customer");

        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Main Warehouse");

        productVariant = new ProductVariant();
        productVariant.setId(UUID.randomUUID());
        productVariant.setSku("TEST-SKU");

        request = new SalesOrderRequest();
        request.setCustomerId(customer.getId());
        request.setWarehouseId(warehouse.getId());
        request.setCurrency("USD");

        SalesOrderItemRequest itemRequest = new SalesOrderItemRequest();
        itemRequest.setProductVariantId(productVariant.getId());
        itemRequest.setQuantity(new BigDecimal("10"));
        itemRequest.setUnitPrice(new BigDecimal("50.00"));

        request.setItems(Collections.singletonList(itemRequest));
    }

    @Test
    void createSalesOrder_Success() {
        when(customerRepository.findById(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(warehouseRepository.findById(request.getWarehouseId())).thenReturn(Optional.of(warehouse));
        when(productVariantRepository.findAllById(any())).thenReturn(Collections.singletonList(productVariant));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder so = invocation.getArgument(0);
            so.setId(UUID.randomUUID());
            return so;
        });

        SalesOrderDto result = salesOrderService.createSalesOrder(request);

        assertNotNull(result);
        assertEquals(SalesOrderStatus.DRAFT, result.getStatus());
        assertEquals(new BigDecimal("500.00"), result.getTotalAmount()); // 10 * 50
        verify(salesOrderRepository).save(any(SalesOrder.class));
    }

    @Test
    void updateSalesOrderStatus_Confirmed_WithStock() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        order.setStatus(SalesOrderStatus.DRAFT);
        order.setWarehouse(warehouse);
        order.setCustomer(customer);
        order.setSoNumber("SO-123");
        order.setPriority(OrderPriority.MEDIUM);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductVariant(productVariant);
        item.setQuantity(new BigDecimal("10"));
        item.setSalesOrder(order);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setTotalPrice(new BigDecimal("500.00"));
        order.setItems(Collections.singletonList(item));

        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stockReservationService.getAvailableToPromise(productVariant.getId(), warehouse.getId()))
                .thenReturn(new BigDecimal("100")); // Plenty of stock
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(order);

        SalesOrderDto result = salesOrderService.updateSalesOrderStatus(orderId, SalesOrderStatus.CONFIRMED);

        assertEquals(SalesOrderStatus.CONFIRMED, result.getStatus());
        verify(stockReservationService, times(1)).getAvailableToPromise(productVariant.getId(), warehouse.getId());
        verify(stockReservationService, times(1)).reserveStock(any(StockReservationRequest.class));
    }

    @Test
    void updateSalesOrderStatus_Confirmed_InsufficientStock() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        order.setStatus(SalesOrderStatus.DRAFT);
        order.setWarehouse(warehouse);
        order.setCustomer(customer);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductVariant(productVariant);
        item.setQuantity(new BigDecimal("10"));
        item.setSalesOrder(order);
        order.setItems(Collections.singletonList(item));

        when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stockReservationService.getAvailableToPromise(productVariant.getId(), warehouse.getId()))
                .thenReturn(new BigDecimal("5")); // Not enough stock

        assertThrows(BadRequestException.class, () ->
            salesOrderService.updateSalesOrderStatus(orderId, SalesOrderStatus.CONFIRMED)
        );

        verify(stockReservationService, never()).reserveStock(any(StockReservationRequest.class));
    }
}
