package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.SupplierDocumentDto;
import com.inventory.system.service.FileStorageService;
import com.inventory.system.service.SupplierDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SupplierDocumentController {

    private final SupplierDocumentService supplierDocumentService;
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/suppliers/{supplierId}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SupplierDocumentDto>> uploadDocument(
            @PathVariable UUID supplierId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "notes", required = false) String notes) {
        SupplierDocumentDto document = supplierDocumentService.uploadDocument(supplierId, file, documentType, notes);
        return new ResponseEntity<>(ApiResponse.success(document, "Supplier document uploaded successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/suppliers/{supplierId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<SupplierDocumentDto>>> getSupplierDocuments(@PathVariable UUID supplierId) {
        List<SupplierDocumentDto> documents = supplierDocumentService.getSupplierDocuments(supplierId);
        return ResponseEntity.ok(ApiResponse.success(documents, "Supplier documents retrieved successfully"));
    }

    @GetMapping("/supplier-documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<SupplierDocumentDto>> getDocument(@PathVariable UUID documentId) {
        SupplierDocumentDto document = supplierDocumentService.getDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(document, "Supplier document retrieved successfully"));
    }

    @GetMapping("/supplier-documents/{documentId}/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<InputStreamResource> getDocumentFile(@PathVariable UUID documentId) {
        SupplierDocumentDto document = supplierDocumentService.getDocument(documentId);
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(document.getStoragePath()));

        MediaType mediaType = (document.getContentType() != null && !document.getContentType().isBlank())
            ? MediaType.parseMediaType(document.getContentType())
            : MediaTypeFactory.getMediaType(document.getFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/supplier-documents/{documentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        supplierDocumentService.deleteDocument(documentId);
        return new ResponseEntity<>(new ApiResponse<>(true, "Supplier document deleted successfully", null), HttpStatus.NO_CONTENT);
    }
}