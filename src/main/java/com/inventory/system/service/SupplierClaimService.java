package com.inventory.system.service;

import com.inventory.system.payload.CreateSupplierClaimRequest;
import com.inventory.system.payload.SupplierClaimDto;

import java.util.List;
import java.util.UUID;

public interface SupplierClaimService {
    SupplierClaimDto createSupplierClaim(UUID goodsReceiptNoteId, CreateSupplierClaimRequest request);
    SupplierClaimDto getSupplierClaim(UUID id);
    List<SupplierClaimDto> getClaimsForGoodsReceipt(UUID goodsReceiptNoteId);
    SupplierClaimDto createSupplierReturnFromClaim(UUID id);
}