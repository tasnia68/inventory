package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.repository.SalesOrderItemRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.impl.ShipmentServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private SalesOrder salesOrder;
    private Warehouse warehouse;
    private SalesOrderItem salesOrderItem;
    private ProductVariant productVariant;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setName("Main Warehouse");

        ProductTemplate template = new ProductTemplate();
        template.setName("Product A");

        productVariant = new ProductVariant();
        productVariant.setId(UUID.randomUUID());
        productVariant.setSku("SKU-001");
        productVariant.setTemplate(template);

        salesOrder = new SalesOrder();
        salesOrder.setId(UUID.randomUUID());
        salesOrder.setSoNumber("SO-001");
        salesOrder.setWarehouse(warehouse);
        salesOrder.setStatus(SalesOrderStatus.CONFIRMED);

        salesOrderItem = new SalesOrderItem();
        salesOrderItem.setId(UUID.randomUUID());
        salesOrderItem.setSalesOrder(salesOrder);
        salesOrderItem.setProductVariant(productVariant);
        salesOrderItem.setQuantity(new BigDecimal("10.00"));
        salesOrderItem.setShippedQuantity(BigDecimal.ZERO);

        salesOrder.setItems(List.of(salesOrderItem));
    }

    @Test
    void createShipment_ShouldCreateShipment_WhenValidRequest() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setSalesOrderId(salesOrder.getId());
        request.setCarrier("DHL");

        CreateShipmentRequest.ShipmentItemRequest itemRequest = new CreateShipmentRequest.ShipmentItemRequest();
        itemRequest.setSalesOrderItemId(salesOrderItem.getId());
        itemRequest.setQuantity(new BigDecimal("5.00"));
        request.setItems(List.of(itemRequest));

        when(salesOrderRepository.findById(salesOrder.getId())).thenReturn(Optional.of(salesOrder));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ShipmentDto result = shipmentService.createShipment(request);

        assertNotNull(result);
        assertEquals(salesOrder.getId(), result.getSalesOrderId());
        assertEquals("DHL", result.getCarrier());
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("5.00"), result.getItems().get(0).getQuantity());
        assertEquals(SalesOrderStatus.PARTIALLY_SHIPPED, salesOrder.getStatus());
    }

    @Test
    void createShipment_ShouldFail_WhenQuantityExceeds() {
        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setSalesOrderId(salesOrder.getId());

        CreateShipmentRequest.ShipmentItemRequest itemRequest = new CreateShipmentRequest.ShipmentItemRequest();
        itemRequest.setSalesOrderItemId(salesOrderItem.getId());
        itemRequest.setQuantity(new BigDecimal("15.00")); // More than 10
        request.setItems(List.of(itemRequest));

        when(salesOrderRepository.findById(salesOrder.getId())).thenReturn(Optional.of(salesOrder));

        assertThrows(RuntimeException.class, () -> shipmentService.createShipment(request));
    }
}
