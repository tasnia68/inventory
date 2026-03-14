package com.inventory.system.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ShareReportRequest {
    @NotNull
    private UUID sharedWithUserId;

    @NotBlank
    private String accessLevel;
}