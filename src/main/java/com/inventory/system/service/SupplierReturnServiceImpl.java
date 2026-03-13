package com.inventory.system.service;

import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import com.inventory.system.common.entity.StockTransactionType;
import com.inventory.system.common.entity.SupplierReturn;
import com.inventory.system.common.entity.SupplierReturnItem;
import com.inventory.system.common.entity.SupplierReturnStatus;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.CreateSupplierReturnItemRequest;
import com.inventory.system.payload.CreateSupplierReturnRequest;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.payload.SupplierReturnDto;
import com.inventory.system.payload.SupplierReturnItemDto;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.SupplierReturnRepository;
import lombok.RequiredArgsConstructor;
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
public class SupplierReturnServiceImpl implements SupplierReturnService {

    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final SupplierReturnRepository supplierReturnRepository;
    private final StockTransactionService stockTransactionService;

    @Override
    @Transactional
    public SupplierReturnDto createSupplierReturn(CreateSupplierReturnRequest request) {
        GoodsReceiptNote goodsReceiptNote = goodsReceiptNoteRepository.findById(request.getGoodsReceiptNoteId())
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", request.getGoodsReceiptNoteId()));

        if (goodsReceiptNote.getStatus() != GoodsReceiptNoteStatus.COMPLETED) {
            throw new BadRequestException("Supplier returns can only be created from completed GRNs");
        }

        Set<UUID> requestedItemIds = request.getItems().stream()
                .map(CreateSupplierReturnItemRequest::getGoodsReceiptNoteItemId)
                .collect(Collectors.toSet());

        Map<UUID, GoodsReceiptNoteItem> goodsReceiptItemMap = goodsReceiptNote.getItems().stream()
                .filter(item -> requestedItemIds.contains(item.getId()))
                .collect(Collectors.toMap(GoodsReceiptNoteItem::getId, Function.identity()));

        if (goodsReceiptItemMap.size() != requestedItemIds.size()) {
            throw new BadRequestException("One or more supplier return items do not belong to the selected GRN");
        }

        SupplierReturn supplierReturn = new SupplierReturn();
        supplierReturn.setReturnNumber(generateReturnNumber());
        supplierReturn.setGoodsReceiptNote(goodsReceiptNote);
        supplierReturn.setSupplier(goodsReceiptNote.getSupplier());
        supplierReturn.setWarehouse(goodsReceiptNote.getWarehouse());
        supplierReturn.setStatus(SupplierReturnStatus.REQUESTED);
        supplierReturn.setReason(request.getReason());
        supplierReturn.setNotes(request.getNotes());
        supplierReturn.setRequestedAt(LocalDateTime.now());

        List<SupplierReturnItem> items = new ArrayList<>();
        for (CreateSupplierReturnItemRequest itemRequest : request.getItems()) {
            GoodsReceiptNoteItem goodsReceiptNoteItem = goodsReceiptItemMap.get(itemRequest.getGoodsReceiptNoteItemId());
            int availableToReturn = goodsReceiptNoteItem.getAcceptedQuantity() - goodsReceiptNoteItem.getReturnedQuantity();
            if (itemRequest.getQuantity() > availableToReturn) {
                throw new BadRequestException("Return quantity exceeds available accepted quantity for GRN item " + goodsReceiptNoteItem.getId());
            }

            SupplierReturnItem returnItem = new SupplierReturnItem();
            returnItem.setSupplierReturn(supplierReturn);
            returnItem.setGoodsReceiptNoteItem(goodsReceiptNoteItem);
            returnItem.setProductVariant(goodsReceiptNoteItem.getProductVariant());
            returnItem.setQuantity(BigDecimal.valueOf(itemRequest.getQuantity()));
            returnItem.setUnitCost(goodsReceiptNoteItem.getPurchaseOrderItem().getUnitPrice());
            returnItem.setReason(itemRequest.getReason());
            items.add(returnItem);
        }

        supplierReturn.setItems(items);
        return mapToDto(supplierReturnRepository.save(supplierReturn));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierReturnDto getSupplierReturn(UUID id) {
        SupplierReturn supplierReturn = supplierReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierReturn", "id", id));
        return mapToDto(supplierReturn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierReturnDto> getReturnsForGoodsReceipt(UUID goodsReceiptNoteId) {
        if (!goodsReceiptNoteRepository.existsById(goodsReceiptNoteId)) {
            throw new ResourceNotFoundException("GoodsReceiptNote", "id", goodsReceiptNoteId);
        }

        return supplierReturnRepository.findByGoodsReceiptNoteIdOrderByCreatedAtDesc(goodsReceiptNoteId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupplierReturnDto confirmSupplierReturn(UUID id) {
        SupplierReturn supplierReturn = supplierReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierReturn", "id", id));

        if (supplierReturn.getStatus() != SupplierReturnStatus.REQUESTED) {
            throw new BadRequestException("Only requested supplier returns can be confirmed");
        }

        CreateStockTransactionRequest transactionRequest = new CreateStockTransactionRequest();
        transactionRequest.setType(StockTransactionType.OUTBOUND);
        transactionRequest.setSourceWarehouseId(supplierReturn.getWarehouse().getId());
        transactionRequest.setReference(supplierReturn.getReturnNumber());
        transactionRequest.setNotes("Generated from supplier return: " + supplierReturn.getReturnNumber());

        List<CreateStockTransactionRequest.ItemRequest> transactionItems = new ArrayList<>();
        for (SupplierReturnItem item : supplierReturn.getItems()) {
            CreateStockTransactionRequest.ItemRequest transactionItem = new CreateStockTransactionRequest.ItemRequest();
            transactionItem.setProductVariantId(item.getProductVariant().getId());
            transactionItem.setQuantity(item.getQuantity());
            transactionItem.setUnitCost(item.getUnitCost());
            transactionItems.add(transactionItem);

            GoodsReceiptNoteItem goodsReceiptNoteItem = item.getGoodsReceiptNoteItem();
            goodsReceiptNoteItem.setReturnedQuantity(goodsReceiptNoteItem.getReturnedQuantity() + item.getQuantity().intValue());
        }

        transactionRequest.setItems(transactionItems);
        StockTransactionDto transaction = stockTransactionService.createTransaction(transactionRequest);
        stockTransactionService.confirmTransaction(transaction.getId());

        supplierReturn.setStatus(SupplierReturnStatus.COMPLETED);
        supplierReturn.setCompletedAt(LocalDateTime.now());
        return mapToDto(supplierReturnRepository.save(supplierReturn));
    }

    @Override
    @Transactional
    public SupplierReturnDto cancelSupplierReturn(UUID id) {
        SupplierReturn supplierReturn = supplierReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierReturn", "id", id));

        if (supplierReturn.getStatus() != SupplierReturnStatus.REQUESTED) {
            throw new BadRequestException("Only requested supplier returns can be cancelled");
        }

        supplierReturn.setStatus(SupplierReturnStatus.CANCELLED);
        return mapToDto(supplierReturnRepository.save(supplierReturn));
    }

    private SupplierReturnDto mapToDto(SupplierReturn supplierReturn) {
        SupplierReturnDto dto = new SupplierReturnDto();
        dto.setId(supplierReturn.getId());
        dto.setReturnNumber(supplierReturn.getReturnNumber());
        dto.setGoodsReceiptNoteId(supplierReturn.getGoodsReceiptNote().getId());
        dto.setGoodsReceiptNoteNumber(supplierReturn.getGoodsReceiptNote().getGrnNumber());
        dto.setSupplierId(supplierReturn.getSupplier().getId());
        dto.setSupplierName(supplierReturn.getSupplier().getName());
        dto.setWarehouseId(supplierReturn.getWarehouse().getId());
        dto.setWarehouseName(supplierReturn.getWarehouse().getName());
        dto.setStatus(supplierReturn.getStatus());
        dto.setReason(supplierReturn.getReason());
        dto.setNotes(supplierReturn.getNotes());
        dto.setRequestedAt(supplierReturn.getRequestedAt());
        dto.setCompletedAt(supplierReturn.getCompletedAt());
        dto.setItems(supplierReturn.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private SupplierReturnItemDto mapItemToDto(SupplierReturnItem item) {
        SupplierReturnItemDto dto = new SupplierReturnItemDto();
        dto.setId(item.getId());
        dto.setGoodsReceiptNoteItemId(item.getGoodsReceiptNoteItem().getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setQuantity(item.getQuantity());
        dto.setUnitCost(item.getUnitCost());
        dto.setReason(item.getReason());
        return dto;
    }

    private String generateReturnNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SRET-" + datePart + "-" + uuidPart;
    }
}