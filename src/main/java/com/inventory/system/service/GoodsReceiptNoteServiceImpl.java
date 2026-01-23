package com.inventory.system.service;

import com.inventory.system.common.entity.*;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.*;
import com.inventory.system.repository.*;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockTransactionService stockTransactionService;
    private final PurchaseOrderService purchaseOrderService;

    @Override
    @Transactional
    public GoodsReceiptNoteDto createGrn(CreateGoodsReceiptNoteRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", request.getPurchaseOrderId()));

        if (po.getStatus() == PurchaseOrderStatus.COMPLETED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot create GRN for Completed or Cancelled PO");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));

        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnNumber(generateGrnNumber());
        grn.setPurchaseOrder(po);
        grn.setSupplier(po.getSupplier());
        grn.setWarehouse(warehouse);
        grn.setReceivedDate(LocalDateTime.now());
        grn.setStatus(GoodsReceiptNoteStatus.DRAFT);
        grn.setNotes(request.getNotes());

        List<GoodsReceiptNoteItem> items = new ArrayList<>();
        for (PurchaseOrderItem poItem : po.getItems()) {
            int remaining = poItem.getQuantity() - poItem.getReceivedQuantity();
            if (remaining > 0) {
                GoodsReceiptNoteItem grnItem = new GoodsReceiptNoteItem();
                grnItem.setGoodsReceiptNote(grn);
                grnItem.setPurchaseOrderItem(poItem);
                grnItem.setProductVariant(poItem.getProductVariant());
                grnItem.setReceivedQuantity(remaining);
                grnItem.setAcceptedQuantity(remaining); // Default to all accepted
                grnItem.setRejectedQuantity(0);
                items.add(grnItem);
            }
        }
        grn.setItems(items);

        GoodsReceiptNote savedGrn = grnRepository.save(grn);
        return mapToDto(savedGrn);
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptNoteDto getGrn(UUID id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", id));
        return mapToDto(grn);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoodsReceiptNoteDto> getAllGrns(GoodsReceiptNoteSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Specification<GoodsReceiptNote> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (request.getGrnNumber() != null && !request.getGrnNumber().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("grnNumber")), "%" + request.getGrnNumber().toUpperCase() + "%"));
            }
            if (request.getPoNumber() != null && !request.getPoNumber().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("purchaseOrder").get("poNumber")), "%" + request.getPoNumber().toUpperCase() + "%"));
            }
            if (request.getSupplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), request.getSupplierId()));
            }
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedDate"), request.getStartDate()));
            }
            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedDate"), request.getEndDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return grnRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional
    public GoodsReceiptNoteDto updateGrnItems(UUID id, List<UpdateGoodsReceiptNoteItemRequest> itemRequests) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", id));

        if (grn.getStatus() == GoodsReceiptNoteStatus.COMPLETED || grn.getStatus() == GoodsReceiptNoteStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update items of a completed or cancelled GRN");
        }

        Map<UUID, UpdateGoodsReceiptNoteItemRequest> requestMap = itemRequests.stream()
                .collect(Collectors.toMap(UpdateGoodsReceiptNoteItemRequest::getId, Function.identity()));

        for (GoodsReceiptNoteItem item : grn.getItems()) {
            if (requestMap.containsKey(item.getId())) {
                UpdateGoodsReceiptNoteItemRequest req = requestMap.get(item.getId());
                if (req.getReceivedQuantity() != null) item.setReceivedQuantity(req.getReceivedQuantity());
                if (req.getAcceptedQuantity() != null) item.setAcceptedQuantity(req.getAcceptedQuantity());
                if (req.getRejectedQuantity() != null) item.setRejectedQuantity(req.getRejectedQuantity());
                if (req.getRejectionReason() != null) item.setRejectionReason(req.getRejectionReason());

                // Validate validation logic
                if (item.getAcceptedQuantity() + item.getRejectedQuantity() != item.getReceivedQuantity()) {
                     throw new IllegalArgumentException("Sum of accepted and rejected quantities must equal received quantity for item " + item.getId());
                }
            }
        }

        grn = grnRepository.save(grn);
        return mapToDto(grn);
    }

    @Override
    @Transactional
    public GoodsReceiptNoteDto confirmGrn(UUID id) {
        GoodsReceiptNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", id));

        if (grn.getStatus() != GoodsReceiptNoteStatus.DRAFT && grn.getStatus() != GoodsReceiptNoteStatus.VERIFIED) {
            throw new IllegalArgumentException("GRN must be in DRAFT or VERIFIED status to confirm");
        }

        // 1. Create Stock Transaction (Inbound)
        CreateStockTransactionRequest trxRequest = new CreateStockTransactionRequest();
        trxRequest.setType(StockTransactionType.INBOUND);
        trxRequest.setDestinationWarehouseId(grn.getWarehouse().getId());
        trxRequest.setReference(grn.getGrnNumber());
        trxRequest.setNotes("Generated from GRN: " + grn.getGrnNumber());

        List<CreateStockTransactionRequest.ItemRequest> trxItems = new ArrayList<>();

        for (GoodsReceiptNoteItem item : grn.getItems()) {
            if (item.getAcceptedQuantity() > 0) {
                CreateStockTransactionRequest.ItemRequest trxItem = new CreateStockTransactionRequest.ItemRequest();
                trxItem.setProductVariantId(item.getProductVariant().getId());
                trxItem.setQuantity(BigDecimal.valueOf(item.getAcceptedQuantity()));
                trxItem.setUnitCost(item.getPurchaseOrderItem().getUnitPrice()); // Use PO Unit Price as Cost
                trxItems.add(trxItem);
            }

            // 2. Update PO Received Quantities
            PurchaseOrderItem poItem = item.getPurchaseOrderItem();
            poItem.setReceivedQuantity(poItem.getReceivedQuantity() + item.getReceivedQuantity());
        }

        trxRequest.setItems(trxItems);

        // Execute Transaction if there are accepted items
        if (!trxItems.isEmpty()) {
            StockTransactionDto trxDto = stockTransactionService.createTransaction(trxRequest);
            stockTransactionService.confirmTransaction(trxDto.getId());
        }

        // 3. Update PO Status
        PurchaseOrder po = grn.getPurchaseOrder();
        // Since poItem is modified and part of po.getItems(), we can check po.getItems()
        boolean allReceived = po.getItems().stream()
                .allMatch(item -> item.getReceivedQuantity() >= item.getQuantity());

        if (allReceived) {
            purchaseOrderService.updatePurchaseOrderStatus(po.getId(), PurchaseOrderStatus.COMPLETED);
        } else {
             purchaseOrderService.updatePurchaseOrderStatus(po.getId(), PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        // 4. Update GRN Status
        grn.setStatus(GoodsReceiptNoteStatus.COMPLETED);
        GoodsReceiptNote savedGrn = grnRepository.save(grn);

        return mapToDto(savedGrn);
    }

    private String generateGrnNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "GRN-" + datePart + "-" + uuidPart;
    }

    private GoodsReceiptNoteDto mapToDto(GoodsReceiptNote grn) {
        GoodsReceiptNoteDto dto = new GoodsReceiptNoteDto();
        dto.setId(grn.getId());
        dto.setGrnNumber(grn.getGrnNumber());
        dto.setPurchaseOrderId(grn.getPurchaseOrder().getId());
        dto.setPurchaseOrderNumber(grn.getPurchaseOrder().getPoNumber());
        dto.setSupplierId(grn.getSupplier().getId());
        dto.setSupplierName(grn.getSupplier().getName());
        dto.setWarehouseId(grn.getWarehouse().getId());
        dto.setWarehouseName(grn.getWarehouse().getName());
        dto.setReceivedDate(grn.getReceivedDate());
        dto.setStatus(grn.getStatus());
        dto.setNotes(grn.getNotes());
        dto.setCreatedAt(grn.getCreatedAt());
        dto.setUpdatedAt(grn.getUpdatedAt());
        dto.setCreatedBy(grn.getCreatedBy());
        dto.setUpdatedBy(grn.getUpdatedBy());

        dto.setItems(grn.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private GoodsReceiptNoteItemDto mapItemToDto(GoodsReceiptNoteItem item) {
        GoodsReceiptNoteItemDto dto = new GoodsReceiptNoteItemDto();
        dto.setId(item.getId());
        dto.setPurchaseOrderItemId(item.getPurchaseOrderItem().getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setReceivedQuantity(item.getReceivedQuantity());
        dto.setAcceptedQuantity(item.getAcceptedQuantity());
        dto.setRejectedQuantity(item.getRejectedQuantity());
        dto.setRejectionReason(item.getRejectionReason());
        return dto;
    }
}
