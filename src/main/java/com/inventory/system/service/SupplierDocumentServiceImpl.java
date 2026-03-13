package com.inventory.system.service;

import com.inventory.system.common.entity.Supplier;
import com.inventory.system.common.entity.SupplierDocument;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.SupplierDocumentDto;
import com.inventory.system.repository.SupplierDocumentRepository;
import com.inventory.system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierDocumentServiceImpl implements SupplierDocumentService {

    private final SupplierRepository supplierRepository;
    private final SupplierDocumentRepository supplierDocumentRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public SupplierDocumentDto uploadDocument(UUID supplierId, MultipartFile file, String documentType, String notes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Supplier document file is required");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new BadRequestException("Supplier document type is required");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", supplierId));

        String storagePath = fileStorageService.uploadFile(file, "supplier-documents/" + supplierId);

        SupplierDocument document = new SupplierDocument();
        document.setSupplier(supplier);
        document.setDocumentType(documentType.trim());
        document.setFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setStoragePath(storagePath);
        document.setNotes(notes);

        return mapToDto(supplierDocumentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDocumentDto> getSupplierDocuments(UUID supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Supplier", "id", supplierId);
        }

        return supplierDocumentRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDocumentDto getDocument(UUID documentId) {
        SupplierDocument document = supplierDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDocument", "id", documentId));
        return mapToDto(document);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        SupplierDocument document = supplierDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDocument", "id", documentId));
        fileStorageService.deleteFile(document.getStoragePath());
        supplierDocumentRepository.delete(document);
    }

    private SupplierDocumentDto mapToDto(SupplierDocument document) {
        SupplierDocumentDto dto = new SupplierDocumentDto();
        dto.setId(document.getId());
        dto.setSupplierId(document.getSupplier().getId());
        dto.setSupplierName(document.getSupplier().getName());
        dto.setDocumentType(document.getDocumentType());
        dto.setFilename(document.getFilename());
        dto.setContentType(document.getContentType());
        dto.setStoragePath(document.getStoragePath());
        dto.setNotes(document.getNotes());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        return dto;
    }
}