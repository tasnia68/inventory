package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RefundDocumentDto {
    private UUID salesRefundId;
    private String refundNumber;
    private String creditNoteNumber;
    private String documentContent;
    private LocalDateTime generatedAt;
}