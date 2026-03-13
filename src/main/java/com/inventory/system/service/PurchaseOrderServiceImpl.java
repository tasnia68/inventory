package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.PurchaseOrder;
import com.inventory.system.common.entity.PurchaseOrderItem;
import com.inventory.system.common.entity.PurchaseOrderStatus;
import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.entity.SupplierStatus;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.PurchaseOrderDto;
import com.inventory.system.payload.PurchaseOrderItemDto;
import com.inventory.system.payload.PurchaseOrderItemRequest;
import com.inventory.system.payload.PurchaseOrderRequest;
import com.inventory.system.payload.PurchaseOrderSearchRequest;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.PurchaseOrderRepository;
import com.inventory.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        validateSupplierEligibility(supplier);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        purchaseOrder.setCurrency(request.getCurrency());
        purchaseOrder.setNotes(request.getNotes());
        purchaseOrder.setOrderDate(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrderStatus.PENDING);
        purchaseOrder.setPoNumber(generatePoNumber());

        Set<UUID> variantIds = request.getItems().stream()
                .map(PurchaseOrderItemRequest::getProductVariantId)
                .collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("Some product variants were not found");
        }

        List<PurchaseOrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            ProductVariant productVariant = variantMap.get(itemRequest.getProductVariantId());

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setProductVariant(productVariant);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setTotalPrice(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            item.setReceivedQuantity(0);

            items.add(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        purchaseOrder.setItems(items);
        purchaseOrder.setTotalAmount(totalAmount);

        PurchaseOrder savedPo = purchaseOrderRepository.save(purchaseOrder);
        return mapToDto(savedPo);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(UUID id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        return mapToDto(purchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getAllPurchaseOrders(PurchaseOrderSearchRequest searchRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (searchRequest.getSupplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), searchRequest.getSupplierId()));
            }
            if (searchRequest.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchRequest.getStatus()));
            }
            if (searchRequest.getPoNumber() != null && !searchRequest.getPoNumber().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("poNumber")), "%" + searchRequest.getPoNumber().toUpperCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll(spec, pageable);
        return purchaseOrders.map(this::mapToDto);
    }

    @Override
    @Transactional
    public PurchaseOrderDto updatePurchaseOrder(UUID id, PurchaseOrderRequest request) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new BadRequestException("Cannot update Purchase Order that is not in PENDING status");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));
        validateSupplierEligibility(supplier);
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        purchaseOrder.setCurrency(request.getCurrency());
        purchaseOrder.setNotes(request.getNotes());

        // Clear existing items and re-add (simple approach, better might be to diff)
        purchaseOrder.getItems().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Set<UUID> variantIds = request.getItems().stream()
                .map(PurchaseOrderItemRequest::getProductVariantId)
                .collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        if (variantMap.size() != variantIds.size()) {
            throw new BadRequestException("Some product variants were not found");
        }

        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            ProductVariant productVariant = variantMap.get(itemRequest.getProductVariantId());

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(purchaseOrder);
            item.setProductVariant(productVariant);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setTotalPrice(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            item.setReceivedQuantity(0);

            purchaseOrder.getItems().add(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }
        purchaseOrder.setTotalAmount(totalAmount);

        PurchaseOrder savedPo = purchaseOrderRepository.save(purchaseOrder);
        return mapToDto(savedPo);
    }

    @Override
    @Transactional
    public PurchaseOrderDto updatePurchaseOrderStatus(UUID id, PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        validateStatusTransition(purchaseOrder.getStatus(), status);
        purchaseOrder.setStatus(status);

        PurchaseOrder savedPo = purchaseOrderRepository.save(purchaseOrder);
        return mapToDto(savedPo);
    }

    @Override
    public void deletePurchaseOrder(UUID id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new BadRequestException("Cannot delete Purchase Order that is not in PENDING status");
        }
        purchaseOrderRepository.delete(purchaseOrder);
    }

    private void validateSupplierEligibility(Supplier supplier) {
        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new BadRequestException("Purchase orders can only be created for active suppliers");
        }
        if (supplier.getStatus() != SupplierStatus.APPROVED) {
            throw new BadRequestException("Purchase orders can only be created for approved suppliers");
        }
    }

    private void validateStatusTransition(PurchaseOrderStatus currentStatus, PurchaseOrderStatus targetStatus) {
        if (targetStatus == currentStatus) {
            return;
        }

        boolean valid = switch (currentStatus) {
            case PENDING -> targetStatus == PurchaseOrderStatus.APPROVED
                    || targetStatus == PurchaseOrderStatus.REJECTED
                    || targetStatus == PurchaseOrderStatus.CANCELLED;
            case APPROVED -> targetStatus == PurchaseOrderStatus.ISSUED
                    || targetStatus == PurchaseOrderStatus.CANCELLED;
            case ISSUED -> targetStatus == PurchaseOrderStatus.PARTIALLY_RECEIVED
                    || targetStatus == PurchaseOrderStatus.COMPLETED
                    || targetStatus == PurchaseOrderStatus.CANCELLED;
            case PARTIALLY_RECEIVED -> targetStatus == PurchaseOrderStatus.COMPLETED
                    || targetStatus == PurchaseOrderStatus.CLOSED;
            case COMPLETED -> targetStatus == PurchaseOrderStatus.CLOSED;
            case REJECTED, CANCELLED, CLOSED -> false;
        };

        if (!valid) {
            throw new BadRequestException("Invalid purchase order status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    private String generatePoNumber() {
        // Format: PO-YYYYMMDD-UUID8
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PO-" + datePart + "-" + uuidPart;
    }

    private PurchaseOrderDto mapToDto(PurchaseOrder po) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setPoNumber(po.getPoNumber());
        dto.setSupplierId(po.getSupplier().getId());
        dto.setSupplierName(po.getSupplier().getName());
        dto.setOrderDate(po.getOrderDate());
        dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
        dto.setStatus(po.getStatus());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCurrency(po.getCurrency());
        dto.setNotes(po.getNotes());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        dto.setCreatedBy(po.getCreatedBy());
        dto.setUpdatedBy(po.getUpdatedBy());

        List<PurchaseOrderItemDto> itemDtos = po.getItems().stream().map(item -> {
            PurchaseOrderItemDto itemDto = new PurchaseOrderItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductVariantId(item.getProductVariant().getId());
            itemDto.setProductVariantName(item.getProductVariant().getSku()); // Assuming SKU as name/identifier
            itemDto.setSku(item.getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setTotalPrice(item.getTotalPrice());
            itemDto.setReceivedQuantity(item.getReceivedQuantity());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
