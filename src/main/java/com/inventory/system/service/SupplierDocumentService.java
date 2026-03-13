package com.inventory.system.service;

import com.inventory.system.payload.SupplierDocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface SupplierDocumentService {
    SupplierDocumentDto uploadDocument(UUID supplierId, MultipartFile file, String documentType, String notes);
    List<SupplierDocumentDto> getSupplierDocuments(UUID supplierId);
    SupplierDocumentDto getDocument(UUID documentId);
    void deleteDocument(UUID documentId);
}