package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreatePickingListRequest;
import com.inventory.system.payload.PickingListDto;
import com.inventory.system.payload.UpdatePickingTaskRequest;
import com.inventory.system.service.PickingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/picking-lists")
@RequiredArgsConstructor
public class PickingController {

    private final PickingService pickingService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PickingListDto>> createPickingList(@Valid @RequestBody CreatePickingListRequest request) {
        PickingListDto pickingList = pickingService.createPickingList(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Picking list generated successfully", pickingList), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PickingListDto>> getPickingList(@PathVariable UUID id) {
        PickingListDto pickingList = pickingService.getPickingList(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Picking list retrieved successfully", pickingList), HttpStatus.OK);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<PickingListDto>> assignPicker(@PathVariable UUID id, @RequestParam UUID userId) {
        PickingListDto pickingList = pickingService.assignPicker(id, userId);
        return new ResponseEntity<>(new ApiResponse<>(true, "Picker assigned successfully", pickingList), HttpStatus.OK);
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<PickingListDto>> updatePickingTask(@PathVariable UUID taskId, @Valid @RequestBody UpdatePickingTaskRequest request) {
        PickingListDto pickingList = pickingService.updatePickingTask(taskId, request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Picking task updated successfully", pickingList), HttpStatus.OK);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PickingListDto>> completePickingList(@PathVariable UUID id) {
        PickingListDto pickingList = pickingService.completePickingList(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Picking list completed successfully", pickingList), HttpStatus.OK);
    }

    @GetMapping("/{id}/packing-list")
    public ResponseEntity<ApiResponse<PickingListDto>> getPackingList(@PathVariable UUID id) {
        PickingListDto pickingList = pickingService.getPickingList(id);
        return new ResponseEntity<>(new ApiResponse<>(true, "Packing list generated successfully", pickingList), HttpStatus.OK);
    }
}
