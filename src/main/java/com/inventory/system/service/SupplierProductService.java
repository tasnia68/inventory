package com.inventory.system.service;

import com.inventory.system.payload.CreateSupplierProductRequest;
import com.inventory.system.payload.SupplierProductDto;
import com.inventory.system.payload.UpdateSupplierProductRequest;

import java.util.List;
import java.util.UUID;

public interface SupplierProductService {
    SupplierProductDto createSupplierProduct(CreateSupplierProductRequest request);
    SupplierProductDto updateSupplierProduct(UUID id, UpdateSupplierProductRequest request);
    SupplierProductDto getSupplierProductById(UUID id);
    List<SupplierProductDto> getProductsBySupplier(UUID supplierId);
    List<SupplierProductDto> getSuppliersByProduct(UUID productVariantId);
    void deleteSupplierProduct(UUID id);
}
