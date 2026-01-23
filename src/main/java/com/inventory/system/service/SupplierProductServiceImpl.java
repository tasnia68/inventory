package com.inventory.system.service;

import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.entity.SupplierProduct;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateSupplierProductRequest;
import com.inventory.system.payload.SupplierProductDto;
import com.inventory.system.payload.UpdateSupplierProductRequest;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SupplierProductRepository;
import com.inventory.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierProductServiceImpl implements SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public SupplierProductDto createSupplierProduct(CreateSupplierProductRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        ProductVariant productVariant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Variant not found with id: " + request.getProductVariantId()));

        SupplierProduct supplierProduct = new SupplierProduct();
        supplierProduct.setSupplier(supplier);
        supplierProduct.setProductVariant(productVariant);
        supplierProduct.setSupplierSku(request.getSupplierSku());
        supplierProduct.setPrice(request.getPrice());
        supplierProduct.setCurrency(request.getCurrency());
        supplierProduct.setLeadTimeDays(request.getLeadTimeDays());

        SupplierProduct savedSupplierProduct = supplierProductRepository.save(supplierProduct);
        return mapToDto(savedSupplierProduct);
    }

    @Override
    @Transactional
    public SupplierProductDto updateSupplierProduct(UUID id, UpdateSupplierProductRequest request) {
        SupplierProduct supplierProduct = supplierProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Product not found with id: " + id));

        if (request.getSupplierSku() != null) {
            supplierProduct.setSupplierSku(request.getSupplierSku());
        }
        if (request.getPrice() != null) {
            supplierProduct.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            supplierProduct.setCurrency(request.getCurrency());
        }
        if (request.getLeadTimeDays() != null) {
            supplierProduct.setLeadTimeDays(request.getLeadTimeDays());
        }

        SupplierProduct updatedSupplierProduct = supplierProductRepository.save(supplierProduct);
        return mapToDto(updatedSupplierProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierProductDto getSupplierProductById(UUID id) {
        SupplierProduct supplierProduct = supplierProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Product not found with id: " + id));
        return mapToDto(supplierProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierProductDto> getProductsBySupplier(UUID supplierId) {
        return supplierProductRepository.findBySupplierId(supplierId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierProductDto> getSuppliersByProduct(UUID productVariantId) {
        return supplierProductRepository.findByProductVariantId(productVariantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSupplierProduct(UUID id) {
        SupplierProduct supplierProduct = supplierProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier Product not found with id: " + id));
        supplierProductRepository.delete(supplierProduct);
    }

    private SupplierProductDto mapToDto(SupplierProduct entity) {
        SupplierProductDto dto = new SupplierProductDto();
        dto.setId(entity.getId());
        dto.setSupplierId(entity.getSupplier().getId());
        dto.setSupplierName(entity.getSupplier().getName());
        dto.setProductVariantId(entity.getProductVariant().getId());
        // Handling lazy loading - in real app might need careful fetch or just ID
        // For now, assuming session open or using ID is enough, but to get name we access getter
        // which might trigger lazy load. In tests this is fine with Transactional.
        dto.setProductVariantName(entity.getProductVariant().getSku()); // or getProductTemplate().getName() if reachable
        dto.setSupplierSku(entity.getSupplierSku());
        dto.setPrice(entity.getPrice());
        dto.setCurrency(entity.getCurrency());
        dto.setLeadTimeDays(entity.getLeadTimeDays());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
