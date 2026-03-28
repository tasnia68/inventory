package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontPublishVersionDto {
    private String id;
    private Integer versionNumber;
    private String label;
    private String note;
    private String publishedAt;
    private Integer restoredFromVersionNumber;
    private String status;
}
