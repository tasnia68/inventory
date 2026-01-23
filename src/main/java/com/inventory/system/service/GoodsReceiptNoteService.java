package com.inventory.system.service;

import com.inventory.system.payload.CreateGoodsReceiptNoteRequest;
import com.inventory.system.payload.GoodsReceiptNoteDto;
import com.inventory.system.payload.GoodsReceiptNoteSearchRequest;
import com.inventory.system.payload.UpdateGoodsReceiptNoteItemRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptNoteService {
    GoodsReceiptNoteDto createGrn(CreateGoodsReceiptNoteRequest request);
    GoodsReceiptNoteDto getGrn(UUID id);
    Page<GoodsReceiptNoteDto> getAllGrns(GoodsReceiptNoteSearchRequest request);
    GoodsReceiptNoteDto updateGrnItems(UUID id, List<UpdateGoodsReceiptNoteItemRequest> items);
    GoodsReceiptNoteDto confirmGrn(UUID id);
}
