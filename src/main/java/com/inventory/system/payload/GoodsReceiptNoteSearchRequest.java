package com.inventory.system.payload;

import com.inventory.system.common.entity.GoodsReceiptNoteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GoodsReceiptNoteSearchRequest {
    private String grnNumber;
    private String poNumber;
    private UUID supplierId;
    private GoodsReceiptNoteStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private int page = 0;
    private int size = 10;
    private String sortBy = "receivedDate";
    private String sortDirection = "DESC";
}
