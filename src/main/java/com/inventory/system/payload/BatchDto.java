package com.inventory.system.payload;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class BatchDto {
    private UUID id;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private UUID productVariantId;
}
