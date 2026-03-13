package com.inventory.system.payload;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportResultDto {
    private int totalRows;
    private int imported;
    private int failed;
    private List<String> errors = new ArrayList<>();
}