package com.inventory.system.payload;

import lombok.Data;

@Data
public class DataExchangeTemplateDto {
    private DataExchangeDataset dataset;
    private String fileName;
    private String contentType;
    private String templateContent;
}