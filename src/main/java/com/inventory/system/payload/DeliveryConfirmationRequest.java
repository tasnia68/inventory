package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryConfirmationRequest {
    private LocalDateTime deliveredAt;
    private String recipientName;
    private String proofOfDeliveryUrl;
    private String notes;
}