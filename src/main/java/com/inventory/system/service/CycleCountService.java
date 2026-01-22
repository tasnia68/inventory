package com.inventory.system.service;

import com.inventory.system.payload.CreateCycleCountRequest;
import com.inventory.system.payload.CycleCountDto;
import com.inventory.system.payload.CycleCountEntryRequest;
import com.inventory.system.payload.CycleCountItemDto;
import com.inventory.system.payload.UpdateCycleCountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CycleCountService {
    CycleCountDto createCycleCount(CreateCycleCountRequest request);
    Page<CycleCountDto> getCycleCounts(Pageable pageable);
    CycleCountDto getCycleCount(UUID id);
    CycleCountDto updateCycleCount(UUID id, UpdateCycleCountRequest request);
    CycleCountDto startCycleCount(UUID id);
    List<CycleCountItemDto> getCycleCountItems(UUID id);
    void enterCount(UUID id, List<CycleCountEntryRequest> entries);
    CycleCountDto finishCycleCount(UUID id);
    CycleCountDto approveCycleCount(UUID id);
}
