package com.inventory.system.payload;

import com.inventory.system.common.entity.PayrollPayFrequency;
import com.inventory.system.common.entity.PayrollRunStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PayrollRunDto {
    private UUID id;
    private String runNumber;
    private String title;
    private PayrollPayFrequency payFrequency;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private PayrollRunStatus status;
    private String currency;
    private String notes;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private UUID journalEntryId;
    private List<PayrollRunItemDto> items = new ArrayList<>();
}
