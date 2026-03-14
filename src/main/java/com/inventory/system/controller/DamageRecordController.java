package com.inventory.system.controller;

import com.inventory.system.common.entity.DamageRecordStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateDamageRecordRequest;
import com.inventory.system.payload.DamageRecordDto;
import com.inventory.system.payload.DamageRecordDocumentDto;
import com.inventory.system.payload.DamageRecordReportDto;
import com.inventory.system.payload.DamageRecordSummaryDto;
import com.inventory.system.service.DamageRecordService;
import com.inventory.system.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/damage-records")
@RequiredArgsConstructor
public class DamageRecordController {

    private final DamageRecordService damageRecordService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<DamageRecordDto>> createDamageRecord(@Valid @RequestBody CreateDamageRecordRequest request) {
        DamageRecordDto record = damageRecordService.createDamageRecord(request);
        return ResponseEntity.created(URI.create("/api/v1/damage-records/" + record.getId()))
                .body(ApiResponse.success(record, "Damage record created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DamageRecordDto>> getDamageRecord(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.getDamageRecord(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DamageRecordDto>>> getDamageRecords(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) DamageRecordStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DamageRecordDto> records = damageRecordService.getDamageRecords(warehouseId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<DamageRecordReportDto>>> getDamageReport(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) DamageRecordStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        List<DamageRecordReportDto> report = damageRecordService.getDamageReport(warehouseId, status, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(report, "Damage report retrieved successfully"));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<DamageRecordSummaryDto>> getDamageSummary(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        DamageRecordSummaryDto summary = damageRecordService.getDamageSummary(warehouseId, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Damage summary retrieved successfully"));
    }

    @PostMapping("/{id}/submit-approval")
    public ResponseEntity<ApiResponse<DamageRecordDto>> submitForApproval(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.submitForApproval(id);
        return ResponseEntity.ok(ApiResponse.success(record, "Damage record submitted for approval"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<DamageRecordDto>> approveDamageRecord(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.approveDamageRecord(id);
        return ResponseEntity.ok(ApiResponse.success(record, "Damage record approved"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<DamageRecordDto>> rejectDamageRecord(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.rejectDamageRecord(id);
        return ResponseEntity.ok(ApiResponse.success(record, "Damage record rejected"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<DamageRecordDto>> confirmDamageRecord(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.confirmDamageRecord(id);
        return ResponseEntity.ok(ApiResponse.success(record, "Damage record confirmed successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<DamageRecordDto>> cancelDamageRecord(@PathVariable UUID id) {
        DamageRecordDto record = damageRecordService.cancelDamageRecord(id);
        return ResponseEntity.ok(ApiResponse.success(record, "Damage record cancelled successfully"));
    }

    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DamageRecordDocumentDto>> uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "notes", required = false) String notes) {
        DamageRecordDocumentDto document = damageRecordService.uploadDocument(id, file, documentType, notes);
        return new ResponseEntity<>(ApiResponse.success(document, "Damage document uploaded successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<DamageRecordDocumentDto>>> getDocuments(@PathVariable UUID id) {
        List<DamageRecordDocumentDto> documents = damageRecordService.getDocuments(id);
        return ResponseEntity.ok(ApiResponse.success(documents, "Damage documents retrieved successfully"));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<DamageRecordDocumentDto>> getDocument(@PathVariable UUID documentId) {
        DamageRecordDocumentDto document = damageRecordService.getDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(document, "Damage document retrieved successfully"));
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<InputStreamResource> getDocumentFile(@PathVariable UUID documentId) {
        DamageRecordDocumentDto document = damageRecordService.getDocument(documentId);
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(document.getStoragePath()));

        MediaType mediaType = (document.getContentType() != null && !document.getContentType().isBlank())
                ? MediaType.parseMediaType(document.getContentType())
                : MediaTypeFactory.getMediaType(document.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFilename() + "\"")
                .body(resource);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        damageRecordService.deleteDocument(documentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Damage document deleted successfully", null));
    }
}