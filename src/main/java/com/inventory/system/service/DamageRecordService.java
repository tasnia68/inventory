package com.inventory.system.service;

import com.inventory.system.common.entity.DamageRecordStatus;
import com.inventory.system.payload.CreateDamageRecordRequest;
import com.inventory.system.payload.CreateDamageRecordFromGrnRequest;
import com.inventory.system.payload.CreateDamageRecordFromRmaRequest;
import com.inventory.system.payload.DamageRecordDto;
import com.inventory.system.payload.DamageRecordDocumentDto;
import com.inventory.system.payload.DamageRecordReportDto;
import com.inventory.system.payload.DamageRecordSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DamageRecordService {
    DamageRecordDto createDamageRecord(CreateDamageRecordRequest request);
    DamageRecordDto createDamageRecordFromGoodsReceipt(UUID goodsReceiptNoteId, CreateDamageRecordFromGrnRequest request);
    DamageRecordDto createDamageRecordFromRma(UUID rmaId, CreateDamageRecordFromRmaRequest request);
    DamageRecordDto getDamageRecord(UUID id);
    Page<DamageRecordDto> getDamageRecords(UUID warehouseId, DamageRecordStatus status, Pageable pageable);
    List<DamageRecordReportDto> getDamageReport(UUID warehouseId, DamageRecordStatus status, LocalDate fromDate, LocalDate toDate);
    DamageRecordSummaryDto getDamageSummary(UUID warehouseId, LocalDate fromDate, LocalDate toDate);
    DamageRecordDto submitForApproval(UUID id);
    DamageRecordDto approveDamageRecord(UUID id);
    DamageRecordDto rejectDamageRecord(UUID id);
    DamageRecordDto confirmDamageRecord(UUID id);
    DamageRecordDto cancelDamageRecord(UUID id);
    DamageRecordDocumentDto uploadDocument(UUID damageRecordId, MultipartFile file, String documentType, String notes);
    List<DamageRecordDocumentDto> getDocuments(UUID damageRecordId);
    DamageRecordDocumentDto getDocument(UUID documentId);
    void deleteDocument(UUID documentId);
}