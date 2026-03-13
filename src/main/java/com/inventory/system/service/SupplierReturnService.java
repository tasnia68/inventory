package com.inventory.system.service;

import com.inventory.system.payload.CreateSupplierReturnRequest;
import com.inventory.system.payload.SupplierReturnDto;

import java.util.List;
import java.util.UUID;

public interface SupplierReturnService {
    SupplierReturnDto createSupplierReturn(CreateSupplierReturnRequest request);
    SupplierReturnDto getSupplierReturn(UUID id);
    List<SupplierReturnDto> getReturnsForGoodsReceipt(UUID goodsReceiptNoteId);
    SupplierReturnDto confirmSupplierReturn(UUID id);
    SupplierReturnDto cancelSupplierReturn(UUID id);
}