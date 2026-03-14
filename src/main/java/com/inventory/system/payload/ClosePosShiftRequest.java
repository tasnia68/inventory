package com.inventory.system.payload;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClosePosShiftRequest {
    private String closingNotes;

    @Valid
    private List<PosShiftTenderCountRequest> tenderCounts = new ArrayList<>();
}