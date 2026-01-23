package com.inventory.system.controller;

import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateGoodsReceiptNoteRequest;
import com.inventory.system.payload.GoodsReceiptNoteDto;
import com.inventory.system.payload.GoodsReceiptNoteSearchRequest;
import com.inventory.system.payload.UpdateGoodsReceiptNoteItemRequest;
import com.inventory.system.service.GoodsReceiptNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goods-receipt-notes")
@RequiredArgsConstructor
@Validated
public class GoodsReceiptNoteController {

    private final GoodsReceiptNoteService grnService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDto>> createGrn(@Valid @RequestBody CreateGoodsReceiptNoteRequest request) {
        GoodsReceiptNoteDto grn = grnService.createGrn(request);
        return new ResponseEntity<>(ApiResponse.success(grn, "GRN created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDto>> getGrn(@PathVariable UUID id) {
        GoodsReceiptNoteDto grn = grnService.getGrn(id);
        return ResponseEntity.ok(ApiResponse.success(grn, "GRN retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GoodsReceiptNoteDto>>> getAllGrns(
            @RequestParam(required = false) String grnNumber,
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) GoodsReceiptNoteStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "receivedDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        GoodsReceiptNoteSearchRequest searchRequest = new GoodsReceiptNoteSearchRequest();
        searchRequest.setGrnNumber(grnNumber);
        searchRequest.setPoNumber(poNumber);
        searchRequest.setSupplierId(supplierId);
        searchRequest.setStatus(status);
        searchRequest.setStartDate(startDate);
        searchRequest.setEndDate(endDate);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);

        Page<GoodsReceiptNoteDto> grns = grnService.getAllGrns(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(grns, "GRNs retrieved successfully"));
    }

    @PutMapping("/{id}/items")
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDto>> updateGrnItems(
            @PathVariable UUID id,
            @RequestBody List<@Valid UpdateGoodsReceiptNoteItemRequest> items) {
        GoodsReceiptNoteDto grn = grnService.updateGrnItems(id, items);
        return ResponseEntity.ok(ApiResponse.success(grn, "GRN items updated successfully"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDto>> confirmGrn(@PathVariable UUID id) {
        GoodsReceiptNoteDto grn = grnService.confirmGrn(id);
        return ResponseEntity.ok(ApiResponse.success(grn, "GRN confirmed successfully"));
    }
}
