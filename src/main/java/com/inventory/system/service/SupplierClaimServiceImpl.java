package com.inventory.system.service;

import com.inventory.system.common.entity.DamageRecord;
import com.inventory.system.common.entity.DamageRecordSourceType;
import com.inventory.system.common.entity.GoodsReceiptNote;
import com.inventory.system.common.entity.GoodsReceiptNoteItem;
import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import com.inventory.system.common.entity.SupplierClaim;
import com.inventory.system.common.entity.SupplierClaimItem;
import com.inventory.system.common.entity.SupplierClaimStatus;
import com.inventory.system.common.entity.SupplierClaimType;
import com.inventory.system.common.entity.SupplierReturn;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateSupplierClaimItemRequest;
import com.inventory.system.payload.CreateSupplierClaimRequest;
import com.inventory.system.payload.CreateSupplierReturnItemRequest;
import com.inventory.system.payload.CreateSupplierReturnRequest;
import com.inventory.system.payload.SupplierClaimDto;
import com.inventory.system.payload.SupplierClaimItemDto;
import com.inventory.system.payload.SupplierReturnDto;
import com.inventory.system.repository.DamageRecordRepository;
import com.inventory.system.repository.GoodsReceiptNoteRepository;
import com.inventory.system.repository.SupplierClaimRepository;
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
public class SupplierClaimServiceImpl implements SupplierClaimService {

    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final DamageRecordRepository damageRecordRepository;
    private final SupplierClaimRepository supplierClaimRepository;
    private final SupplierReturnRepository supplierReturnRepository;
    private final SupplierReturnService supplierReturnService;

    @Override
    @Transactional
    public SupplierClaimDto createSupplierClaim(UUID goodsReceiptNoteId, CreateSupplierClaimRequest request) {
        GoodsReceiptNote goodsReceiptNote = goodsReceiptNoteRepository.findById(goodsReceiptNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("GoodsReceiptNote", "id", goodsReceiptNoteId));

        if (goodsReceiptNote.getStatus() != GoodsReceiptNoteStatus.VERIFIED && goodsReceiptNote.getStatus() != GoodsReceiptNoteStatus.COMPLETED) {
            throw new BadRequestException("Supplier claims can only be created from verified or completed GRNs");
        }

        DamageRecord damageRecord = null;
        if (request.getDamageRecordId() != null) {
            damageRecord = damageRecordRepository.findById(request.getDamageRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("DamageRecord", "id", request.getDamageRecordId()));
            validateDamageRecordLink(goodsReceiptNote, damageRecord);
        }

        Set<UUID> requestedItemIds = request.getItems().stream()
                .map(CreateSupplierClaimItemRequest::getGoodsReceiptNoteItemId)
                .collect(Collectors.toSet());

        Map<UUID, GoodsReceiptNoteItem> goodsReceiptItemMap = goodsReceiptNote.getItems().stream()
                .filter(item -> requestedItemIds.contains(item.getId()))
                .collect(Collectors.toMap(GoodsReceiptNoteItem::getId, Function.identity()));

        if (goodsReceiptItemMap.size() != requestedItemIds.size()) {
            throw new BadRequestException("One or more supplier claim items do not belong to the selected GRN");
        }

        Map<UUID, BigDecimal> damageQuantityByVariant = damageRecord == null
                ? Map.of()
                : damageRecord.getItems().stream().collect(Collectors.groupingBy(
                        item -> item.getProductVariant().getId(),
                        Collectors.mapping(
                                item -> item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        Map<UUID, BigDecimal> requestedQuantityByVariant = request.getItems().stream().collect(Collectors.groupingBy(
                item -> goodsReceiptItemMap.get(item.getGoodsReceiptNoteItemId()).getProductVariant().getId(),
                Collectors.mapping(item -> BigDecimal.valueOf(item.getQuantity()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
        ));

        if (damageRecord != null) {
            for (Map.Entry<UUID, BigDecimal> entry : requestedQuantityByVariant.entrySet()) {
                BigDecimal maxDamageQuantity = damageQuantityByVariant.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                if (entry.getValue().compareTo(maxDamageQuantity) > 0) {
                    throw new BadRequestException("Supplier claim quantity exceeds damaged quantity for one or more products");
                }
            }
        }

        SupplierClaim claim = new SupplierClaim();
        claim.setClaimNumber(generateClaimNumber());
        claim.setGoodsReceiptNote(goodsReceiptNote);
        claim.setSupplier(goodsReceiptNote.getSupplier());
        claim.setWarehouse(goodsReceiptNote.getWarehouse());
        claim.setDamageRecord(damageRecord);
        claim.setStatus(SupplierClaimStatus.OPEN);
        claim.setClaimType(resolveClaimType(request, damageRecord));
        claim.setReason(request.getReason());
        claim.setNotes(request.getNotes());
        claim.setClaimedAt(LocalDateTime.now());

        List<SupplierClaimItem> items = new ArrayList<>();
        for (CreateSupplierClaimItemRequest itemRequest : request.getItems()) {
            GoodsReceiptNoteItem goodsReceiptNoteItem = goodsReceiptItemMap.get(itemRequest.getGoodsReceiptNoteItemId());
            BigDecimal requestedQuantity = BigDecimal.valueOf(itemRequest.getQuantity());
            BigDecimal rejectedQuantity = BigDecimal.valueOf(goodsReceiptNoteItem.getRejectedQuantity());
            if (requestedQuantity.compareTo(rejectedQuantity) > 0) {
                throw new BadRequestException("Claim quantity exceeds rejected quantity for GRN item " + goodsReceiptNoteItem.getId());
            }

            SupplierClaimItem item = new SupplierClaimItem();
            item.setSupplierClaim(claim);
            item.setGoodsReceiptNoteItem(goodsReceiptNoteItem);
            item.setProductVariant(goodsReceiptNoteItem.getProductVariant());
            item.setQuantity(requestedQuantity);
            item.setUnitCost(goodsReceiptNoteItem.getPurchaseOrderItem().getUnitPrice());
            item.setClaimedAmount(item.getUnitCost() == null ? null : item.getUnitCost().multiply(requestedQuantity));
            item.setReason(itemRequest.getReason());
            items.add(item);
        }

        claim.setItems(items);
        return mapToDto(supplierClaimRepository.save(claim));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierClaimDto getSupplierClaim(UUID id) {
        return mapToDto(getSupplierClaimEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierClaimDto> getClaimsForGoodsReceipt(UUID goodsReceiptNoteId) {
        if (!goodsReceiptNoteRepository.existsById(goodsReceiptNoteId)) {
            throw new ResourceNotFoundException("GoodsReceiptNote", "id", goodsReceiptNoteId);
        }

        return supplierClaimRepository.findByGoodsReceiptNoteIdOrderByCreatedAtDesc(goodsReceiptNoteId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public SupplierClaimDto createSupplierReturnFromClaim(UUID id) {
        SupplierClaim claim = getSupplierClaimEntity(id);
        if (claim.getSupplierReturn() != null) {
            throw new BadRequestException("A supplier return is already linked to this claim");
        }
        if (claim.getStatus() != SupplierClaimStatus.OPEN) {
            throw new BadRequestException("Only open supplier claims can generate supplier returns");
        }

        CreateSupplierReturnRequest request = new CreateSupplierReturnRequest();
        request.setGoodsReceiptNoteId(claim.getGoodsReceiptNote().getId());
        request.setReason(claim.getReason());
        request.setNotes(buildSupplierReturnNotes(claim));
        request.setItems(claim.getItems().stream().map(item -> {
            CreateSupplierReturnItemRequest returnItem = new CreateSupplierReturnItemRequest();
            returnItem.setGoodsReceiptNoteItemId(item.getGoodsReceiptNoteItem().getId());
            returnItem.setQuantity(toWholeQuantity(item.getQuantity()));
            returnItem.setReason(item.getReason());
            return returnItem;
        }).toList());

        SupplierReturnDto supplierReturnDto = supplierReturnService.createSupplierReturn(request);
        SupplierReturn supplierReturn = supplierReturnRepository.findById(supplierReturnDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SupplierReturn", "id", supplierReturnDto.getId()));

        claim.setSupplierReturn(supplierReturn);
        claim.setStatus(SupplierClaimStatus.RETURN_REQUESTED);
        return mapToDto(supplierClaimRepository.save(claim));
    }

    private SupplierClaim getSupplierClaimEntity(UUID id) {
        return supplierClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierClaim", "id", id));
    }

    private void validateDamageRecordLink(GoodsReceiptNote goodsReceiptNote, DamageRecord damageRecord) {
        if (damageRecord.getSourceType() != DamageRecordSourceType.RECEIVING) {
            throw new BadRequestException("Only receiving damage records can be linked to supplier claims");
        }
        if (!damageRecord.getWarehouse().getId().equals(goodsReceiptNote.getWarehouse().getId())) {
            throw new BadRequestException("Damage record warehouse does not match the selected GRN");
        }
        if (damageRecord.getReference() == null || !damageRecord.getReference().equals(goodsReceiptNote.getGrnNumber())) {
            throw new BadRequestException("Damage record is not linked to the selected GRN");
        }
    }

    private SupplierClaimType resolveClaimType(CreateSupplierClaimRequest request, DamageRecord damageRecord) {
        if (request.getClaimType() != null) {
            return request.getClaimType();
        }
        if (damageRecord != null) {
            return SupplierClaimType.DAMAGED_RECEIPT;
        }
        return SupplierClaimType.REJECTED_RECEIPT;
    }

    private int toWholeQuantity(BigDecimal quantity) {
        try {
            return quantity.intValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Supplier return creation requires whole-number claim quantities");
        }
    }

    private String buildSupplierReturnNotes(SupplierClaim claim) {
        String claimNote = "Generated from supplier claim: " + claim.getClaimNumber();
        if (claim.getNotes() == null || claim.getNotes().isBlank()) {
            return claimNote;
        }
        return claimNote + ". " + claim.getNotes();
    }

    private SupplierClaimDto mapToDto(SupplierClaim claim) {
        SupplierClaimDto dto = new SupplierClaimDto();
        dto.setId(claim.getId());
        dto.setClaimNumber(claim.getClaimNumber());
        dto.setGoodsReceiptNoteId(claim.getGoodsReceiptNote().getId());
        dto.setGoodsReceiptNoteNumber(claim.getGoodsReceiptNote().getGrnNumber());
        dto.setSupplierId(claim.getSupplier().getId());
        dto.setSupplierName(claim.getSupplier().getName());
        dto.setWarehouseId(claim.getWarehouse().getId());
        dto.setWarehouseName(claim.getWarehouse().getName());
        if (claim.getDamageRecord() != null) {
            dto.setDamageRecordId(claim.getDamageRecord().getId());
            dto.setDamageRecordNumber(claim.getDamageRecord().getRecordNumber());
        }
        if (claim.getSupplierReturn() != null) {
            dto.setSupplierReturnId(claim.getSupplierReturn().getId());
            dto.setSupplierReturnNumber(claim.getSupplierReturn().getReturnNumber());
        }
        dto.setStatus(claim.getStatus());
        dto.setClaimType(claim.getClaimType());
        dto.setReason(claim.getReason());
        dto.setNotes(claim.getNotes());
        dto.setClaimedAt(claim.getClaimedAt());
        dto.setResolvedAt(claim.getResolvedAt());
        dto.setItems(claim.getItems().stream().map(this::mapItemToDto).toList());
        return dto;
    }

    private SupplierClaimItemDto mapItemToDto(SupplierClaimItem item) {
        SupplierClaimItemDto dto = new SupplierClaimItemDto();
        dto.setId(item.getId());
        dto.setGoodsReceiptNoteItemId(item.getGoodsReceiptNoteItem().getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantSku(item.getProductVariant().getSku());
        dto.setQuantity(item.getQuantity());
        dto.setUnitCost(item.getUnitCost());
        dto.setClaimedAmount(item.getClaimedAmount());
        dto.setReason(item.getReason());
        return dto;
    }

    private String generateClaimNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SCLM-" + datePart + "-" + uuidPart;
    }
}