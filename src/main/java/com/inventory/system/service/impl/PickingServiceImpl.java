package com.inventory.system.service.impl;

import com.inventory.system.common.entity.PickingList;
import com.inventory.system.common.entity.PickingStatus;
import com.inventory.system.common.entity.PickingTask;
import com.inventory.system.common.entity.PickingType;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.Stock;
import com.inventory.system.common.entity.User;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.payload.CreatePickingListRequest;
import com.inventory.system.payload.PickingListDto;
import com.inventory.system.payload.PickingTaskDto;
import com.inventory.system.payload.UpdatePickingTaskRequest;
import com.inventory.system.repository.PickingListRepository;
import com.inventory.system.repository.PickingTaskRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.service.PickingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PickingServiceImpl implements PickingService {

    private final PickingListRepository pickingListRepository;
    private final PickingTaskRepository pickingTaskRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    @Override
    public PickingListDto createPickingList(CreatePickingListRequest request) {
        List<SalesOrder> salesOrders = salesOrderRepository.findAllById(request.getSalesOrderIds());
        if (salesOrders.size() != request.getSalesOrderIds().size()) {
            throw new ResourceNotFoundException("One or more Sales Orders not found");
        }

        if (salesOrders.isEmpty()) {
             throw new BadRequestException("No sales orders provided");
        }

        UUID warehouseId = salesOrders.get(0).getWarehouse().getId();
        for (SalesOrder so : salesOrders) {
            if (!so.getWarehouse().getId().equals(warehouseId)) {
                throw new BadRequestException("All Sales Orders must belong to the same warehouse");
            }
            if (so.getStatus() != com.inventory.system.common.entity.SalesOrderStatus.CONFIRMED) {
                 throw new BadRequestException("Sales Order " + so.getSoNumber() + " is not in CONFIRMED status");
            }
        }

        User assignedTo = null;
        if (request.getAssignedToId() != null) {
            assignedTo = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedToId()));
        }

        PickingList pickingList = new PickingList();
        pickingList.setPickingNumber(generatePickingNumber());
        pickingList.setWarehouse(salesOrders.get(0).getWarehouse());
        pickingList.setAssignedTo(assignedTo);
        pickingList.setStatus(PickingStatus.DRAFT);
        pickingList.setType(salesOrders.size() > 1 ? PickingType.WAVE : PickingType.SINGLE_ORDER);
        pickingList.setNotes(request.getNotes());

        List<PickingTask> tasks = new ArrayList<>();

        for (SalesOrder so : salesOrders) {
            for (SalesOrderItem item : so.getItems()) {
                BigDecimal remainingQuantity = item.getQuantity().subtract(item.getShippedQuantity());
                if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Find stock
                List<Stock> stocks = stockRepository.findByProductVariantIdAndWarehouseIdAndQuantityGreaterThan(
                        item.getProductVariant().getId(), warehouseId, BigDecimal.ZERO);

                // Sort by location name for efficient picking path?
                // stocks.sort(Comparator.comparing(s -> s.getStorageLocation().getName()));

                for (Stock stock : stocks) {
                    if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) break;

                    if (stock.getStorageLocation() == null) continue;

                    BigDecimal pickQuantity = stock.getQuantity().min(remainingQuantity);

                    PickingTask task = new PickingTask();
                    task.setPickingList(pickingList);
                    task.setSalesOrderItem(item);
                    task.setProductVariant(item.getProductVariant());
                    task.setStorageLocation(stock.getStorageLocation());
                    task.setBatch(stock.getBatch());
                    task.setRequestedQuantity(pickQuantity);
                    task.setPickedQuantity(BigDecimal.ZERO);
                    task.setStatus(PickingStatus.DRAFT);

                    tasks.add(task);
                    remainingQuantity = remainingQuantity.subtract(pickQuantity);
                }
            }
        }

        if (tasks.isEmpty()) {
             throw new BadRequestException("No picking tasks could be created. Check stock availability.");
        }

        pickingList.setTasks(tasks);
        PickingList savedList = pickingListRepository.save(pickingList);

        return mapToDto(savedList);
    }

    @Override
    @Transactional(readOnly = true)
    public PickingListDto getPickingList(UUID id) {
        PickingList pickingList = pickingListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Picking List", "id", id));
        return mapToDto(pickingList);
    }

    @Override
    public PickingListDto assignPicker(UUID pickingListId, UUID userId) {
        PickingList pickingList = pickingListRepository.findById(pickingListId)
                .orElseThrow(() -> new ResourceNotFoundException("Picking List", "id", pickingListId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        pickingList.setAssignedTo(user);
        if (pickingList.getStatus() == PickingStatus.DRAFT) {
            pickingList.setStatus(PickingStatus.ASSIGNED);
        }

        PickingList savedList = pickingListRepository.save(pickingList);
        return mapToDto(savedList);
    }

    @Override
    public PickingListDto updatePickingTask(UUID taskId, UpdatePickingTaskRequest request) {
        PickingTask task = pickingTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Picking Task", "id", taskId));

        if (request.getPickedQuantity().compareTo(task.getRequestedQuantity()) > 0) {
             throw new BadRequestException("Picked quantity cannot exceed requested quantity");
        }

        task.setPickedQuantity(request.getPickedQuantity());
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        if (task.getPickedQuantity().compareTo(task.getRequestedQuantity()) >= 0) {
            task.setStatus(PickingStatus.COMPLETED);
        } else if (task.getPickedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            task.setStatus(PickingStatus.IN_PROGRESS);
        }

        // Update parent list status
        PickingList list = task.getPickingList();
        if (list.getStatus() == PickingStatus.ASSIGNED || list.getStatus() == PickingStatus.DRAFT) {
            list.setStatus(PickingStatus.IN_PROGRESS);
            pickingListRepository.save(list);
        }

        pickingTaskRepository.save(task);
        return mapToDto(list);
    }

    @Override
    public PickingListDto completePickingList(UUID pickingListId) {
        PickingList pickingList = pickingListRepository.findById(pickingListId)
                .orElseThrow(() -> new ResourceNotFoundException("Picking List", "id", pickingListId));

        boolean allCompleted = pickingList.getTasks().stream()
                .allMatch(t -> t.getStatus() == PickingStatus.COMPLETED || t.getPickedQuantity().compareTo(t.getRequestedQuantity()) >= 0);

        if (!allCompleted) {
             throw new BadRequestException("All tasks must be completed before completing the picking list");
        }

        pickingList.setStatus(PickingStatus.COMPLETED);
        PickingList savedList = pickingListRepository.save(pickingList);
        return mapToDto(savedList);
    }

    private String generatePickingNumber() {
        return "PL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PickingListDto mapToDto(PickingList entity) {
        PickingListDto dto = new PickingListDto();
        dto.setId(entity.getId());
        dto.setPickingNumber(entity.getPickingNumber());
        dto.setWarehouseId(entity.getWarehouse().getId());
        dto.setWarehouseName(entity.getWarehouse().getName());
        if (entity.getAssignedTo() != null) {
            dto.setAssignedToId(entity.getAssignedTo().getId());
            dto.setAssignedToName(entity.getAssignedTo().getEmail());
        }
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getType());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getTasks() != null) {
            dto.setTasks(entity.getTasks().stream().map(this::mapTaskToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private PickingTaskDto mapTaskToDto(PickingTask task) {
        PickingTaskDto dto = new PickingTaskDto();
        dto.setId(task.getId());
        dto.setSalesOrderItemId(task.getSalesOrderItem().getId());
        dto.setProductVariantId(task.getProductVariant().getId());
        dto.setProductVariantName(task.getProductVariant().getSku());
        dto.setSku(task.getProductVariant().getSku());
        dto.setStorageLocationId(task.getStorageLocation().getId());
        dto.setStorageLocationName(task.getStorageLocation().getName());
        if (task.getBatch() != null) {
            dto.setBatchId(task.getBatch().getId());
            dto.setBatchNumber(task.getBatch().getBatchNumber());
        }
        dto.setRequestedQuantity(task.getRequestedQuantity());
        dto.setPickedQuantity(task.getPickedQuantity());
        dto.setStatus(task.getStatus());
        dto.setNotes(task.getNotes());
        return dto;
    }
}
