package com.inventory.system.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateGoodsReceiptNoteItemRequest {
    @NotNull
    private UUID id;

    @Min(0)
    private Integer receivedQuantity;

    @Min(0)
    private Integer acceptedQuantity;

    @Min(0)
    private Integer rejectedQuantity;

    private String rejectionReason;

    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private List<String> serialNumbers;
}
