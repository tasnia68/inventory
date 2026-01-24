package com.inventory.system.service.impl;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentItemDto;
import com.inventory.system.payload.UpdateShipmentStatusRequest;
import com.inventory.system.repository.SalesOrderItemRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.ShipmentRepository;
import com.inventory.system.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    @Override
    @Transactional
    public ShipmentDto createShipment(CreateShipmentRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found"));

        if (salesOrder.getStatus() == SalesOrderStatus.DRAFT || salesOrder.getStatus() == SalesOrderStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Sales Order must be confirmed before shipping");
        }
        if (salesOrder.getStatus() == SalesOrderStatus.SHIPPED || salesOrder.getStatus() == SalesOrderStatus.DELIVERED || salesOrder.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BadRequestException("Sales Order is already fully shipped or cancelled");
        }

        Shipment shipment = new Shipment();
        shipment.setSalesOrder(salesOrder);
        shipment.setWarehouse(salesOrder.getWarehouse());
        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNumber(request.getTrackingNumber());
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setShipmentDate(LocalDateTime.now());
        shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        shipment.setNotes(request.getNotes());
        shipment.setShipmentNumber(generateShipmentNumber());

        List<ShipmentItem> shipmentItems = new ArrayList<>();
        Map<UUID, SalesOrderItem> soItemMap = salesOrder.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CreateShipmentRequest.ShipmentItemRequest itemRequest : request.getItems()) {
                SalesOrderItem soItem = soItemMap.get(itemRequest.getSalesOrderItemId());
                if (soItem == null) {
                    throw new BadRequestException("Sales Order Item not found: " + itemRequest.getSalesOrderItemId());
                }
                BigDecimal shippedQty = soItem.getShippedQuantity() != null ? soItem.getShippedQuantity() : BigDecimal.ZERO;
                BigDecimal remaining = soItem.getQuantity().subtract(shippedQty);
                if (itemRequest.getQuantity().compareTo(remaining) > 0) {
                    throw new BadRequestException("Cannot ship more than remaining quantity for item: " + soItem.getProductVariant().getSku());
                }
                if (itemRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Quantity must be positive");
                }

                ShipmentItem shipmentItem = new ShipmentItem();
                shipmentItem.setShipment(shipment);
                shipmentItem.setSalesOrderItem(soItem);
                shipmentItem.setQuantity(itemRequest.getQuantity());
                shipmentItems.add(shipmentItem);

                soItem.setShippedQuantity(shippedQty.add(itemRequest.getQuantity()));
            }
        } else {
            // Ship all remaining
            for (SalesOrderItem soItem : salesOrder.getItems()) {
                BigDecimal shippedQty = soItem.getShippedQuantity() != null ? soItem.getShippedQuantity() : BigDecimal.ZERO;
                BigDecimal remaining = soItem.getQuantity().subtract(shippedQty);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    ShipmentItem shipmentItem = new ShipmentItem();
                    shipmentItem.setShipment(shipment);
                    shipmentItem.setSalesOrderItem(soItem);
                    shipmentItem.setQuantity(remaining);
                    shipmentItems.add(shipmentItem);

                    soItem.setShippedQuantity(soItem.getQuantity());
                }
            }
        }

        if (shipmentItems.isEmpty()) {
            throw new BadRequestException("No items to ship");
        }

        shipment.setItems(shipmentItems);
        Shipment savedShipment = shipmentRepository.save(shipment);

        updateSalesOrderStatus(salesOrder);
        salesOrderRepository.save(salesOrder);

        return mapToDto(savedShipment);
    }

    private void updateSalesOrderStatus(SalesOrder salesOrder) {
        boolean allShipped = true;
        boolean anyShipped = false;
        for (SalesOrderItem item : salesOrder.getItems()) {
            BigDecimal shipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
            if (shipped.compareTo(BigDecimal.ZERO) > 0) {
                anyShipped = true;
            }
            if (shipped.compareTo(item.getQuantity()) < 0) {
                allShipped = false;
            }
        }

        if (allShipped) {
            salesOrder.setStatus(SalesOrderStatus.SHIPPED);
        } else if (anyShipped) {
            salesOrder.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDto getShipment(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        return mapToDto(shipment);
    }

    @Override
    @Transactional
    public ShipmentDto updateShipmentStatus(UUID id, UpdateShipmentStatusRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        shipment.setStatus(request.getStatus());
        if (request.getStatus() == ShipmentStatus.DELIVERED && shipment.getDeliveryDate() == null) {
            shipment.setDeliveryDate(LocalDateTime.now());
            // Check if all shipments for the SO are delivered, then maybe mark SO as DELIVERED?
            // For now, let's keep SO as SHIPPED/PARTIALLY_SHIPPED.
            // Often SO status DELIVERED is updated when all items are delivered.
            checkAndUpdateSalesOrderDelivered(shipment.getSalesOrder());
        }
        if (request.getNotes() != null) {
            shipment.setNotes(request.getNotes());
        }

        return mapToDto(shipmentRepository.save(shipment));
    }

    private void checkAndUpdateSalesOrderDelivered(SalesOrder salesOrder) {
         // This is complex because we need to check ALL shipments for this SO.
         // And also ensure all items are shipped.
         List<Shipment> shipments = shipmentRepository.findBySalesOrderId(salesOrder.getId());
         boolean allDelivered = shipments.stream().allMatch(s -> s.getStatus() == ShipmentStatus.DELIVERED);

         boolean allItemsShipped = salesOrder.getItems().stream()
                 .allMatch(item -> {
                     BigDecimal shipped = item.getShippedQuantity() != null ? item.getShippedQuantity() : BigDecimal.ZERO;
                     return shipped.compareTo(item.getQuantity()) >= 0;
                 });

         if (allDelivered && allItemsShipped) {
             salesOrder.setStatus(SalesOrderStatus.DELIVERED);
             salesOrderRepository.save(salesOrder);
         }
    }

    @Override
    public String generateLabel(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        // Mock label generation
        return "MOCK-LABEL-" + shipment.getShipmentNumber();
    }

    @Override
    public String generateDeliveryNote(UUID id) {
         Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        StringBuilder note = new StringBuilder();
        note.append("DELIVERY NOTE\n");
        note.append("Shipment #: ").append(shipment.getShipmentNumber()).append("\n");
        note.append("Date: ").append(shipment.getShipmentDate()).append("\n");
        note.append("To: ").append(shipment.getSalesOrder().getCustomer().getName()).append("\n\n");
        note.append("Items:\n");
        for (ShipmentItem item : shipment.getItems()) {
            note.append("- ").append(item.getSalesOrderItem().getProductVariant().getSku())
                .append(" x ").append(item.getQuantity()).append("\n");
        }
        return note.toString();
    }

    private String generateShipmentNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SH-" + datePart + "-" + uuidPart;
    }

    private ShipmentDto mapToDto(Shipment shipment) {
        ShipmentDto dto = new ShipmentDto();
        dto.setId(shipment.getId());
        dto.setShipmentNumber(shipment.getShipmentNumber());
        dto.setSalesOrderId(shipment.getSalesOrder().getId());
        dto.setSoNumber(shipment.getSalesOrder().getSoNumber());
        dto.setWarehouseId(shipment.getWarehouse().getId());
        dto.setWarehouseName(shipment.getWarehouse().getName());
        dto.setCarrier(shipment.getCarrier());
        dto.setTrackingNumber(shipment.getTrackingNumber());
        dto.setStatus(shipment.getStatus());
        dto.setShipmentDate(shipment.getShipmentDate());
        dto.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
        dto.setDeliveryDate(shipment.getDeliveryDate());
        dto.setNotes(shipment.getNotes());

        List<ShipmentItemDto> itemDtos = shipment.getItems().stream().map(item -> {
            ShipmentItemDto itemDto = new ShipmentItemDto();
            itemDto.setId(item.getId());
            itemDto.setSalesOrderItemId(item.getSalesOrderItem().getId());
            itemDto.setProductVariantName(item.getSalesOrderItem().getProductVariant().getTemplate().getName());
            itemDto.setProductSku(item.getSalesOrderItem().getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
