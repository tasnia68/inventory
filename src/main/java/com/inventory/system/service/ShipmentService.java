package com.inventory.system.service;

import com.inventory.system.common.entity.ShipmentStatus;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.UpdateShipmentStatusRequest;

import java.util.UUID;

public interface ShipmentService {
    ShipmentDto createShipment(CreateShipmentRequest request);
    ShipmentDto getShipment(UUID id);
    ShipmentDto updateShipmentStatus(UUID id, UpdateShipmentStatusRequest request);
    String generateLabel(UUID id);
    String generateDeliveryNote(UUID id);
}
