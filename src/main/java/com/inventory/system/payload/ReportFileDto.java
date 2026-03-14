package com.inventory.system.payload;

import lombok.Data;

@Data
public class ReportFileDto {
    private String fileName;
    private String contentType;
    private byte[] content;
}