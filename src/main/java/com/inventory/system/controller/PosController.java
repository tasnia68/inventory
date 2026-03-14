package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ClosePosShiftRequest;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.CreatePosShiftRequest;
import com.inventory.system.payload.CreatePosTerminalRequest;
import com.inventory.system.payload.PosBootstrapDto;
import com.inventory.system.payload.PosCatalogItemDto;
import com.inventory.system.payload.PosKpiDto;
import com.inventory.system.payload.PosSaleDto;
import com.inventory.system.payload.PosShiftDto;
import com.inventory.system.payload.PosTerminalDto;
import com.inventory.system.payload.UpdatePosTerminalStatusRequest;
import com.inventory.system.service.PosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    @GetMapping("/terminals")
    public ResponseEntity<ApiResponse<List<PosTerminalDto>>> getTerminals(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(posService.getTerminals(includeInactive), "POS terminals retrieved successfully"));
    }

    @PostMapping("/terminals")
    public ResponseEntity<ApiResponse<PosTerminalDto>> createTerminal(@Valid @RequestBody CreatePosTerminalRequest request) {
        return new ResponseEntity<>(ApiResponse.success(posService.createTerminal(request), "POS terminal created successfully"), HttpStatus.CREATED);
    }

    @PatchMapping("/terminals/{terminalId}/status")
    public ResponseEntity<ApiResponse<PosTerminalDto>> updateTerminalStatus(
            @PathVariable UUID terminalId,
            @Valid @RequestBody UpdatePosTerminalStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(posService.updateTerminalStatus(terminalId, request), "POS terminal status updated successfully"));
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<PosBootstrapDto>> getBootstrap(@RequestParam(required = false) UUID terminalId) {
        return ResponseEntity.ok(ApiResponse.success(posService.getBootstrap(terminalId), "POS bootstrap retrieved successfully"));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Page<PosCatalogItemDto>>> searchCatalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int size) {
        return ResponseEntity.ok(ApiResponse.success(posService.searchCatalog(q, categoryId, warehouseId, page, size), "POS catalog retrieved successfully"));
    }

    @GetMapping("/catalog/scan")
    public ResponseEntity<ApiResponse<PosCatalogItemDto>> scanBarcode(
            @RequestParam String barcode,
            @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(posService.scanBarcode(barcode, warehouseId), "POS barcode matched successfully"));
    }

    @PostMapping("/shifts/open")
    public ResponseEntity<ApiResponse<PosShiftDto>> openShift(@Valid @RequestBody CreatePosShiftRequest request) {
        return new ResponseEntity<>(ApiResponse.success(posService.openShift(request), "POS shift opened successfully"), HttpStatus.CREATED);
    }

    @PostMapping("/shifts/{shiftId}/close")
    public ResponseEntity<ApiResponse<PosShiftDto>> closeShift(@PathVariable UUID shiftId, @RequestBody ClosePosShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.success(posService.closeShift(shiftId, request), "POS shift closed successfully"));
    }

    @GetMapping("/shifts/current")
    public ResponseEntity<ApiResponse<PosShiftDto>> getCurrentShift(@RequestParam UUID terminalId) {
        return ResponseEntity.ok(ApiResponse.success(posService.getCurrentShift(terminalId), "POS shift retrieved successfully"));
    }

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<PosSaleDto>> createSale(@Valid @RequestBody CreatePosSaleRequest request) {
        return new ResponseEntity<>(ApiResponse.success(posService.createSale(request, false), "POS sale recorded successfully"), HttpStatus.CREATED);
    }

    @PostMapping("/sales/offline-sync")
    public ResponseEntity<ApiResponse<List<PosSaleDto>>> syncOfflineSales(@Valid @RequestBody List<CreatePosSaleRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success(posService.syncOfflineSales(requests), "Offline POS sales synced successfully"));
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Page<PosSaleDto>>> getSales(
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(posService.getSales(cashierId, terminalId, page, size), "POS sales retrieved successfully"));
    }

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<PosKpiDto>> getKpis(
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return ResponseEntity.ok(ApiResponse.success(posService.getKpis(cashierId, terminalId, businessDate), "POS KPIs retrieved successfully"));
    }
}