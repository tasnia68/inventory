package com.inventory.system.service;

import com.inventory.system.common.entity.PurchaseRequisition;
import com.inventory.system.common.entity.PurchaseRequisitionItem;
import com.inventory.system.common.entity.PurchaseRequisitionStatus;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.GeneratePurchaseRequisitionRequest;
import com.inventory.system.payload.PurchaseRequisitionDto;
import com.inventory.system.payload.PurchaseRequisitionItemDto;
import com.inventory.system.payload.ReplenishmentSuggestionDto;
import com.inventory.system.repository.PurchaseRequisitionItemRepository;
import com.inventory.system.repository.PurchaseRequisitionRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionServiceImpl implements PurchaseRequisitionService {

    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final PurchaseRequisitionItemRepository purchaseRequisitionItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final ReplenishmentService replenishmentService;
    private final ProductVariantRepository productVariantRepository;
    private final com.inventory.system.repository.PurchaseOrderRepository purchaseOrderRepository;
    private final com.inventory.system.repository.SupplierRepository supplierRepository;

    @Override
    @Transactional
    public PurchaseRequisitionDto generate(GeneratePurchaseRequisitionRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        List<ReplenishmentSuggestionDto> suggestions = replenishmentService.getReplenishmentSuggestions(warehouse.getId());

        PurchaseRequisition pr = new PurchaseRequisition();
        pr.setReference(generateReference());
        pr.setWarehouse(warehouse);
        pr.setStatus(PurchaseRequisitionStatus.DRAFT);
        pr.setNotes(request.getNotes());
        pr.setRequestedAt(LocalDateTime.now());

        PurchaseRequisition saved = purchaseRequisitionRepository.save(pr);

        List<PurchaseRequisitionItem> items = suggestions.stream()
                .filter(s -> s.getSuggestedQuantity() != null && s.getSuggestedQuantity().signum() > 0)
                .map(s -> {
                    PurchaseRequisitionItem item = new PurchaseRequisitionItem();
                    item.setPurchaseRequisition(saved);
                    item.setProductVariant(productVariantRepository.getReferenceById(s.getProductVariantId()));
                    item.setQuantity(s.getSuggestedQuantity());
                    item.setSuggestedQuantity(s.getSuggestedQuantity());
                    return item;
                }).collect(Collectors.toList());

        purchaseRequisitionItemRepository.saveAll(items);
        saved.setItems(items);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequisitionDto getById(UUID id) {
        PurchaseRequisition pr = purchaseRequisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequisition", "id", id));
        return mapToDto(pr);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseRequisitionDto> getAll(Pageable pageable) {
        return purchaseRequisitionRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequisitionDto> getByWarehouse(UUID warehouseId) {
        return purchaseRequisitionRepository.findByWarehouseId(warehouseId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public java.util.UUID convertToPurchaseOrder(UUID prId, UUID supplierId, java.time.LocalDate expectedDeliveryDate) {
        PurchaseRequisition pr = purchaseRequisitionRepository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseRequisition", "id", prId));
        if (pr.getStatus() != PurchaseRequisitionStatus.APPROVED) {
            throw new com.inventory.system.common.exception.BadRequestException(
                    "Only APPROVED requisitions can be converted to a PO; current status is " + pr.getStatus());
        }
        if (pr.getConvertedPurchaseOrderId() != null) {
            throw new com.inventory.system.common.exception.BadRequestException(
                    "Requisition " + pr.getReference() + " is already linked to PO " + pr.getConvertedPurchaseOrderId());
        }
        if (pr.getItems() == null || pr.getItems().isEmpty()) {
            throw new com.inventory.system.common.exception.BadRequestException(
                    "Requisition has no line items");
        }

        com.inventory.system.common.entity.Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", supplierId));

        com.inventory.system.common.entity.PurchaseOrder po =
                new com.inventory.system.common.entity.PurchaseOrder();
        po.setSupplier(supplier);
        po.setExpectedDeliveryDate(expectedDeliveryDate);
        po.setOrderDate(LocalDateTime.now());
        po.setStatus(com.inventory.system.common.entity.PurchaseOrderStatus.PENDING);
        po.setPoNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        po.setNotes("Generated from requisition " + pr.getReference()
                + (pr.getNotes() != null ? "\n" + pr.getNotes() : ""));

        java.util.List<com.inventory.system.common.entity.PurchaseOrderItem> poItems = new java.util.ArrayList<>();
        java.math.BigDecimal poTotal = java.math.BigDecimal.ZERO;
        for (PurchaseRequisitionItem prItem : pr.getItems()) {
            com.inventory.system.common.entity.PurchaseOrderItem poItem =
                    new com.inventory.system.common.entity.PurchaseOrderItem();
            poItem.setPurchaseOrder(po);
            poItem.setProductVariant(prItem.getProductVariant());
            int qty = prItem.getQuantity() != null ? prItem.getQuantity().intValue() : 0;
            poItem.setQuantity(qty);
            // Default unit price to zero — operator fills it on the PO before approval.
            poItem.setUnitPrice(java.math.BigDecimal.ZERO);
            poItem.setTotalPrice(java.math.BigDecimal.ZERO);
            poItem.setReceivedQuantity(0);
            poItems.add(poItem);
        }
        po.setItems(poItems);
        po.setTotalAmount(poTotal);
        com.inventory.system.common.entity.PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        pr.setConvertedAt(LocalDateTime.now());
        pr.setConvertedPurchaseOrderId(savedPo.getId());
        purchaseRequisitionRepository.save(pr);

        return savedPo.getId();
    }

    private PurchaseRequisitionDto mapToDto(PurchaseRequisition pr) {
        PurchaseRequisitionDto dto = new PurchaseRequisitionDto();
        dto.setId(pr.getId());
        dto.setReference(pr.getReference());
        dto.setWarehouseId(pr.getWarehouse().getId());
        dto.setWarehouseName(pr.getWarehouse().getName());
        dto.setStatus(pr.getStatus());
        dto.setNotes(pr.getNotes());
        dto.setRequestedAt(pr.getRequestedAt());
        dto.setCreatedAt(pr.getCreatedAt());
        dto.setUpdatedAt(pr.getUpdatedAt());

        List<PurchaseRequisitionItemDto> items = pr.getItems().stream().map(item -> {
            PurchaseRequisitionItemDto itemDto = new PurchaseRequisitionItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductVariantId(item.getProductVariant().getId());
            itemDto.setProductVariantName(item.getProductVariant().getTemplate().getName());
            itemDto.setSku(item.getProductVariant().getSku());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setSuggestedQuantity(item.getSuggestedQuantity());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    private String generateReference() {
        return "PRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
