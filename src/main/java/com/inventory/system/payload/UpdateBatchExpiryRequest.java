package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateBatchExpiryRequest {
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
}
