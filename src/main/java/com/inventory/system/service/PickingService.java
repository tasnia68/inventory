package com.inventory.system.service;

import com.inventory.system.payload.CreatePickingListRequest;
import com.inventory.system.payload.PickingListDto;
import com.inventory.system.payload.UpdatePickingTaskRequest;

import java.util.UUID;

public interface PickingService {
    PickingListDto createPickingList(CreatePickingListRequest request);
    PickingListDto getPickingList(UUID id);
    PickingListDto assignPicker(UUID pickingListId, UUID userId);
    PickingListDto updatePickingTask(UUID taskId, UpdatePickingTaskRequest request);
    PickingListDto completePickingList(UUID pickingListId);
}
