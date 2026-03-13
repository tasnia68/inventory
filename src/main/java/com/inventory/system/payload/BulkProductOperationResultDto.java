package com.inventory.system.payload;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BulkProductOperationResultDto {
    private int totalRequested;
    private int totalUpdated;
    private List<String> errors = new ArrayList<>();
}