package com.inventory.system.service;

import com.inventory.system.payload.CreateSupplierRequest;
import com.inventory.system.payload.SupplierDto;
import com.inventory.system.payload.UpdateSupplierRequest;

import java.util.List;
import java.util.UUID;

public interface SupplierService {
    SupplierDto createSupplier(CreateSupplierRequest request);
    SupplierDto updateSupplier(UUID id, UpdateSupplierRequest request);
    SupplierDto getSupplierById(UUID id);
    List<SupplierDto> getAllSuppliers();
    void deleteSupplier(UUID id);
}
