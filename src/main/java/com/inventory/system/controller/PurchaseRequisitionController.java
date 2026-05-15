package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.GeneratePurchaseRequisitionRequest;
import com.inventory.system.payload.PurchaseRequisitionDto;
import com.inventory.system.service.PurchaseRequisitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-requisitions")
@RequiredArgsConstructor
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService purchaseRequisitionService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PurchaseRequisitionDto>> generate(@Valid @RequestBody GeneratePurchaseRequisitionRequest request) {
        PurchaseRequisitionDto pr = purchaseRequisitionService.generate(request);
        return ResponseEntity.ok(ApiResponse.success(pr, "Purchase requisition generated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseRequisitionDto>> getById(@PathVariable UUID id) {
        PurchaseRequisitionDto pr = purchaseRequisitionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(pr, "Purchase requisition retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PurchaseRequisitionDto>>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Page<PurchaseRequisitionDto> page = purchaseRequisitionService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Purchase requisitions retrieved successfully"));
    }

    @GetMapping("/by-warehouse")
    public ResponseEntity<ApiResponse<List<PurchaseRequisitionDto>>> getByWarehouse(@RequestParam UUID warehouseId) {
        List<PurchaseRequisitionDto> list = purchaseRequisitionService.getByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(list, "Purchase requisitions retrieved successfully"));
    }

    @PostMapping("/{id}/convert-to-po")
    public ResponseEntity<ApiResponse<java.util.Map<String, UUID>>> convertToPurchaseOrder(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body) {
        UUID supplierId = body.get("supplierId") != null ? UUID.fromString(String.valueOf(body.get("supplierId"))) : null;
        if (supplierId == null) {
            throw new com.inventory.system.common.exception.BadRequestException("supplierId is required");
        }
        java.time.LocalDate expectedDeliveryDate = body.get("expectedDeliveryDate") != null
                ? java.time.LocalDate.parse(String.valueOf(body.get("expectedDeliveryDate")))
                : null;
        UUID poId = purchaseRequisitionService.convertToPurchaseOrder(id, supplierId, expectedDeliveryDate);
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("purchaseOrderId", poId),
                "Purchase order created from requisition"
        ));
    }
}
