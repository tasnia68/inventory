package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.ReturnAuthorizationDto;
import com.inventory.system.repository.ReturnAuthorizationRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.impl.RmaServiceImpl;
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
public class RmaServiceImplTest {

    @Mock
    private ReturnAuthorizationRepository rmaRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @InjectMocks
    private RmaServiceImpl rmaService;

    private SalesOrder salesOrder;
    private Customer customer;
    private SalesOrderItem salesOrderItem;
    private ProductVariant productVariant;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("John Doe");

        ProductTemplate template = new ProductTemplate();
        template.setName("Product A");

        productVariant = new ProductVariant();
        productVariant.setId(UUID.randomUUID());
        productVariant.setSku("SKU-001");
        productVariant.setTemplate(template);

        salesOrder = new SalesOrder();
        salesOrder.setId(UUID.randomUUID());
        salesOrder.setSoNumber("SO-001");
        salesOrder.setCustomer(customer);
        salesOrder.setStatus(SalesOrderStatus.SHIPPED);

        salesOrderItem = new SalesOrderItem();
        salesOrderItem.setId(UUID.randomUUID());
        salesOrderItem.setSalesOrder(salesOrder);
        salesOrderItem.setProductVariant(productVariant);
        salesOrderItem.setQuantity(new BigDecimal("10.00"));
        salesOrderItem.setShippedQuantity(new BigDecimal("10.00")); // Assume fully shipped

        salesOrder.setItems(List.of(salesOrderItem));
    }

    @Test
    void createRma_ShouldCreateRma_WhenValidRequest() {
        CreateRmaRequest request = new CreateRmaRequest();
        request.setSalesOrderId(salesOrder.getId());
        request.setReason("Defective");

        CreateRmaRequest.ReturnItemRequest itemRequest = new CreateRmaRequest.ReturnItemRequest();
        itemRequest.setSalesOrderItemId(salesOrderItem.getId());
        itemRequest.setQuantity(new BigDecimal("2.00"));
        request.setItems(List.of(itemRequest));

        when(salesOrderRepository.findById(salesOrder.getId())).thenReturn(Optional.of(salesOrder));
        when(rmaRepository.save(any(ReturnAuthorization.class))).thenAnswer(invocation -> {
            ReturnAuthorization r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReturnAuthorizationDto result = rmaService.createRma(request);

        assertNotNull(result);
        assertEquals(salesOrder.getId(), result.getSalesOrderId());
        assertEquals("Defective", result.getReason());
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("2.00"), result.getItems().get(0).getQuantity());
    }

    @Test
    void createRma_ShouldFail_WhenQuantityExceeds() {
        CreateRmaRequest request = new CreateRmaRequest();
        request.setSalesOrderId(salesOrder.getId());

        CreateRmaRequest.ReturnItemRequest itemRequest = new CreateRmaRequest.ReturnItemRequest();
        itemRequest.setSalesOrderItemId(salesOrderItem.getId());
        itemRequest.setQuantity(new BigDecimal("20.00")); // More than 10
        request.setItems(List.of(itemRequest));

        when(salesOrderRepository.findById(salesOrder.getId())).thenReturn(Optional.of(salesOrder));

        assertThrows(RuntimeException.class, () -> rmaService.createRma(request));
    }
}
