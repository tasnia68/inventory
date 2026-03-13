package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DeliveryNoteDto {
    private UUID shipmentId;
    private String shipmentNumber;
    private String note;
    private LocalDateTime generatedAt;
}