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
