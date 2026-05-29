package com.inventory.system.controller;

import com.inventory.system.payload.ApplyReferralRequest;
import com.inventory.system.payload.ReferralAttributionDto;
import com.inventory.system.payload.ReferralCodeDto;
import com.inventory.system.payload.ReferralProgramDto;
import com.inventory.system.payload.UpsertReferralProgramRequest;
import com.inventory.system.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/program")
    public ResponseEntity<ReferralProgramDto> getProgram() {
        return ResponseEntity.ok(referralService.getProgram());
    }

    @PutMapping("/program")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ReferralProgramDto> upsertProgram(@RequestBody UpsertReferralProgramRequest request) {
        return ResponseEntity.ok(referralService.upsertProgram(request));
    }

    @PostMapping("/codes/customer/{customerId}")
    public ResponseEntity<ReferralCodeDto> getOrCreateCode(@PathVariable UUID customerId) {
        return ResponseEntity.ok(referralService.getOrCreateCodeForCustomer(customerId));
    }

    @GetMapping("/codes/customer/{customerId}")
    public ResponseEntity<List<ReferralCodeDto>> listCodes(@PathVariable UUID customerId) {
        return ResponseEntity.ok(referralService.getCodesForCustomer(customerId));
    }

    @PostMapping("/attribute")
    public ResponseEntity<ReferralAttributionDto> attribute(@RequestBody ApplyReferralRequest request) {
        return ResponseEntity.ok(referralService.attribute(request));
    }

    @GetMapping("/attributions/code/{referralCodeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<ReferralAttributionDto>> attributionsForCode(@PathVariable UUID referralCodeId) {
        return ResponseEntity.ok(referralService.listAttributionsForCode(referralCodeId));
    }
}
