package com.inventory.system.service;

import com.inventory.system.payload.CreatePickingListRequest;
import com.inventory.system.payload.PickingListDto;
import com.inventory.system.payload.UpdatePickingTaskRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PickingService {
    PickingListDto createPickingList(CreatePickingListRequest request);
    Page<PickingListDto> getPickingLists(UUID warehouseId, com.inventory.system.common.entity.PickingStatus status, int page, int size, String sortBy, String sortDirection);
    PickingListDto getPickingList(UUID id);
    PickingListDto assignPicker(UUID pickingListId, UUID userId);
    PickingListDto updatePickingTask(UUID taskId, UpdatePickingTaskRequest request);
    PickingListDto completePickingList(UUID pickingListId);
}
