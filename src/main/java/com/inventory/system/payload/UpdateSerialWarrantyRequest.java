package com.inventory.system.payload;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateSerialWarrantyRequest {
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
}