package com.inventory.system.service.impl;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.ReturnAuthorizationDto;
import com.inventory.system.payload.ReturnItemDto;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.repository.ReturnAuthorizationRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.RmaService;
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
public class RmaServiceImpl implements RmaService {

    private final ReturnAuthorizationRepository rmaRepository;
    private final SalesOrderRepository salesOrderRepository;

    @Override
    @Transactional
    public ReturnAuthorizationDto createRma(CreateRmaRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales Order not found"));

        if (salesOrder.getStatus() == SalesOrderStatus.DRAFT || salesOrder.getStatus() == SalesOrderStatus.PENDING_APPROVAL) {
             throw new BadRequestException("Cannot create RMA for Draft/Pending Sales Order");
        }

        ReturnAuthorization rma = new ReturnAuthorization();
        rma.setRmaNumber(generateRmaNumber());
        rma.setSalesOrder(salesOrder);
        rma.setCustomer(salesOrder.getCustomer());
        rma.setStatus(RmaStatus.REQUESTED);
        rma.setReason(request.getReason());
        rma.setNotes(request.getNotes());
        rma.setRequestDate(LocalDateTime.now());

        List<ReturnItem> rmaItems = new ArrayList<>();
        Map<UUID, SalesOrderItem> soItemMap = salesOrder.getItems().stream()
                .collect(Collectors.toMap(SalesOrderItem::getId, Function.identity()));

        for (CreateRmaRequest.ReturnItemRequest itemRequest : request.getItems()) {
            SalesOrderItem soItem = soItemMap.get(itemRequest.getSalesOrderItemId());
            if (soItem == null) {
                throw new BadRequestException("Sales Order Item not found: " + itemRequest.getSalesOrderItemId());
            }
            if (itemRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity must be positive");
            }
             // Check if quantity exceeds shipped quantity
            BigDecimal shippedQty = soItem.getShippedQuantity() != null ? soItem.getShippedQuantity() : BigDecimal.ZERO;
            if (itemRequest.getQuantity().compareTo(shippedQty) > 0) {
                 throw new BadRequestException("Cannot return more than shipped quantity for item: " + soItem.getProductVariant().getSku());
            }

            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturnAuthorization(rma);
            returnItem.setSalesOrderItem(soItem);
            returnItem.setQuantity(itemRequest.getQuantity());
            returnItem.setCondition(itemRequest.getCondition());
            returnItem.setResolution(itemRequest.getResolution());
            rmaItems.add(returnItem);
        }

        rma.setItems(rmaItems);
        ReturnAuthorization savedRma = rmaRepository.save(rma);

        return mapToDto(savedRma);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnAuthorizationDto getRma(UUID id) {
        ReturnAuthorization rma = rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RMA not found"));
        return mapToDto(rma);
    }

    @Override
    @Transactional
    public ReturnAuthorizationDto updateRmaStatus(UUID id, UpdateRmaStatusRequest request) {
        ReturnAuthorization rma = rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RMA not found"));

        rma.setStatus(request.getStatus());
        if (request.getStatus() == RmaStatus.APPROVED && rma.getApprovedDate() == null) {
            rma.setApprovedDate(LocalDateTime.now());
        }
        if (request.getStatus() == RmaStatus.RECEIVED && rma.getReceivedDate() == null) {
            rma.setReceivedDate(LocalDateTime.now());
            // TODO: Trigger stock update (Inbound Transaction)
        }
        if (request.getNotes() != null) {
            rma.setNotes(request.getNotes());
        }

        return mapToDto(rmaRepository.save(rma));
    }

    private String generateRmaNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RMA-" + datePart + "-" + uuidPart;
    }

    private ReturnAuthorizationDto mapToDto(ReturnAuthorization rma) {
        ReturnAuthorizationDto dto = new ReturnAuthorizationDto();
        dto.setId(rma.getId());
        dto.setRmaNumber(rma.getRmaNumber());
        dto.setSalesOrderId(rma.getSalesOrder().getId());
        dto.setSoNumber(rma.getSalesOrder().getSoNumber());
        dto.setCustomerId(rma.getCustomer().getId());
        dto.setCustomerName(rma.getCustomer().getName());
        dto.setStatus(rma.getStatus());
        dto.setReason(rma.getReason());
        dto.setRequestDate(rma.getRequestDate());
        dto.setApprovedDate(rma.getApprovedDate());
        dto.setReceivedDate(rma.getReceivedDate());
        dto.setNotes(rma.getNotes());

        List<ReturnItemDto> itemDtos = rma.getItems().stream().map(item -> {
            ReturnItemDto itemDto = new ReturnItemDto();
            itemDto.setId(item.getId());
            itemDto.setSalesOrderItemId(item.getSalesOrderItem().getId());
            itemDto.setProductVariantName(item.getSalesOrderItem().getProductVariant().getTemplate().getName());
            itemDto.setProductSku(item.getSalesOrderItem().getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setCondition(item.getCondition());
            itemDto.setResolution(item.getResolution());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
