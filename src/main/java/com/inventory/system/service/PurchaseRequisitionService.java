package com.inventory.system.service;

import com.inventory.system.payload.GeneratePurchaseRequisitionRequest;
import com.inventory.system.payload.PurchaseRequisitionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PurchaseRequisitionService {
    PurchaseRequisitionDto generate(GeneratePurchaseRequisitionRequest request);
    PurchaseRequisitionDto getById(UUID id);
    Page<PurchaseRequisitionDto> getAll(Pageable pageable);
    List<PurchaseRequisitionDto> getByWarehouse(UUID warehouseId);
}
