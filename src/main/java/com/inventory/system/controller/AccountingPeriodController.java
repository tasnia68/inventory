package com.inventory.system.controller;

import com.inventory.system.common.entity.AccountingPeriod;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.service.AccountingPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountingPeriod>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.list()));
    }

    @PostMapping("/close")
    public ResponseEntity<ApiResponse<AccountingPeriod>> close(@RequestBody Map<String, Object> body,
                                                               @AuthenticationPrincipal UserDetails principal) {
        LocalDate start = LocalDate.parse(String.valueOf(body.get("periodStart")));
        LocalDate end = LocalDate.parse(String.valueOf(body.get("periodEnd")));
        String notes = body.get("notes") != null ? String.valueOf(body.get("notes")) : null;
        String actor = principal != null ? principal.getUsername() : "system";
        return ResponseEntity.ok(ApiResponse.success(
                service.close(start, end, notes, actor),
                "Accounting period closed"
        ));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<AccountingPeriod>> reopen(@PathVariable UUID id,
                                                                @AuthenticationPrincipal UserDetails principal) {
        String actor = principal != null ? principal.getUsername() : "system";
        return ResponseEntity.ok(ApiResponse.success(
                service.reopen(id, actor),
                "Accounting period reopened"
        ));
    }
}
