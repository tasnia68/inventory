package com.inventory.system.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopifySyncRunDto {
    private String id;
    private String syncType;
    private String status;       // RUNNING | COMPLETED | FAILED
    private boolean hasMore;     // true while status == RUNNING and more pages remain
    private boolean incremental;
    private String queryFilter;
    private int pagesProcessed;
    private String message;
    private ShopifySyncResultDto result;
    private String startedAt;
    private String finishedAt;
}
