package com.inventory.system.payload;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataImportValidationResultDto {
    private boolean valid;
    private int totalRecords;
    private List<String> errors = new ArrayList<>();
}