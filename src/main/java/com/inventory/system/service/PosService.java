package com.inventory.system.service;

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
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PosService {
    List<PosTerminalDto> getTerminals();
    List<PosTerminalDto> getTerminals(boolean includeInactive);
    PosTerminalDto createTerminal(CreatePosTerminalRequest request);
    PosTerminalDto updateTerminalStatus(UUID terminalId, UpdatePosTerminalStatusRequest request);
    PosBootstrapDto getBootstrap(UUID terminalId);
    Page<PosCatalogItemDto> searchCatalog(String query, UUID categoryId, UUID warehouseId, int page, int size);
    PosCatalogItemDto scanBarcode(String barcode, UUID warehouseId);
    PosShiftDto openShift(CreatePosShiftRequest request);
    PosShiftDto closeShift(UUID shiftId, ClosePosShiftRequest request);
    PosShiftDto getCurrentShift(UUID terminalId);
    PosSaleDto createSale(CreatePosSaleRequest request, boolean offlineSync);
    List<PosSaleDto> syncOfflineSales(List<CreatePosSaleRequest> requests);
    Page<PosSaleDto> getSales(UUID cashierId, UUID terminalId, int page, int size);
    PosKpiDto getKpis(UUID cashierId, UUID terminalId, LocalDate businessDate);
}