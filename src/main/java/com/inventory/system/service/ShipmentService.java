package com.inventory.system.service;

import com.inventory.system.payload.CreateRmaRequest;
import com.inventory.system.payload.CreateShipmentRequest;
import com.inventory.system.payload.DeliveryConfirmationRequest;
import com.inventory.system.payload.DeliveryNoteDto;
import com.inventory.system.payload.GenerateShippingLabelRequest;
import com.inventory.system.payload.RmaDto;
import com.inventory.system.payload.ShipmentDeliveryReviewActionRequest;
import com.inventory.system.payload.ShipmentDto;
import com.inventory.system.payload.ShipmentQueueSummaryDto;
import com.inventory.system.payload.ShipmentSearchRequest;
import com.inventory.system.payload.UpdateRmaStatusRequest;
import com.inventory.system.payload.UpdateShipmentTrackingRequest;
import com.inventory.system.common.entity.ShipmentQueueType;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ShipmentService {
    ShipmentDto createShipment(CreateShipmentRequest request);
    ShipmentDto getShipment(UUID id);
    Page<ShipmentDto> getAllShipments(ShipmentSearchRequest request);
    Page<ShipmentDto> getShipmentsByQueue(ShipmentQueueType queue, int page, int size, String sortBy, String sortDirection);
    ShipmentQueueSummaryDto getShipmentQueueSummary();
    ShipmentDto updateTracking(UUID shipmentId, UpdateShipmentTrackingRequest request);
    ShipmentDto generateShippingLabel(UUID shipmentId, GenerateShippingLabelRequest request);
    ShipmentDto confirmDelivery(UUID shipmentId, DeliveryConfirmationRequest request);
    ShipmentDto approveDelivery(UUID shipmentId, ShipmentDeliveryReviewActionRequest request);
    ShipmentDto disputeDelivery(UUID shipmentId, ShipmentDeliveryReviewActionRequest request);
    DeliveryNoteDto generateDeliveryNote(UUID shipmentId);

    RmaDto createRma(CreateRmaRequest request);
    RmaDto getRma(UUID id);
    Page<RmaDto> getAllRmas(UUID salesOrderId, String rmaNumber, int page, int size, String sortBy, String sortDirection);
    RmaDto updateRmaStatus(UUID id, UpdateRmaStatusRequest request);
}