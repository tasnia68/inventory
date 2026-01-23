package com.inventory.system.service;

import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateSupplierRequest;
import com.inventory.system.payload.SupplierDto;
import com.inventory.system.payload.UpdateSupplierRequest;
import com.inventory.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setName(request.getName());
        supplier.setContactName(request.getContactName());
        supplier.setEmail(request.getEmail());
        supplier.setPhoneNumber(request.getPhoneNumber());
        supplier.setAddress(request.getAddress());
        supplier.setPaymentTerms(request.getPaymentTerms());
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }

        Supplier savedSupplier = supplierRepository.save(supplier);
        return mapToDto(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return mapToDto(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupplierDto updateSupplier(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        if (request.getName() != null) {
            supplier.setName(request.getName());
        }
        if (request.getContactName() != null) {
            supplier.setContactName(request.getContactName());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            supplier.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        if (request.getPaymentTerms() != null) {
            supplier.setPaymentTerms(request.getPaymentTerms());
        }
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }

        Supplier updatedSupplier = supplierRepository.save(supplier);
        return mapToDto(updatedSupplier);
    }

    @Override
    @Transactional
    public void deleteSupplier(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplierRepository.delete(supplier);
    }

    private SupplierDto mapToDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setContactName(supplier.getContactName());
        dto.setEmail(supplier.getEmail());
        dto.setPhoneNumber(supplier.getPhoneNumber());
        dto.setAddress(supplier.getAddress());
        dto.setPaymentTerms(supplier.getPaymentTerms());
        dto.setIsActive(supplier.getIsActive());
        dto.setCreatedAt(supplier.getCreatedAt());
        dto.setUpdatedAt(supplier.getUpdatedAt());
        return dto;
    }
}
