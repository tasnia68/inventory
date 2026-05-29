package com.inventory.system.controller;

import com.inventory.system.payload.AdjustGiftCardRequest;
import com.inventory.system.payload.GiftCardBalanceDto;
import com.inventory.system.payload.GiftCardDto;
import com.inventory.system.payload.GiftCardTransactionDto;
import com.inventory.system.payload.IssueGiftCardRequest;
import com.inventory.system.service.GiftCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gift-cards")
@RequiredArgsConstructor
public class GiftCardController {

    private final GiftCardService giftCardService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<GiftCardDto> issue(@RequestBody IssueGiftCardRequest request) {
        return ResponseEntity.ok(giftCardService.issue(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<GiftCardDto>> list() {
        return ResponseEntity.ok(giftCardService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<GiftCardDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(giftCardService.get(id));
    }

    @GetMapping("/balance/{code}")
    public ResponseEntity<GiftCardBalanceDto> balance(@PathVariable String code) {
        return ResponseEntity.ok(giftCardService.getBalance(code));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<GiftCardDto> adjust(@PathVariable UUID id, @RequestBody AdjustGiftCardRequest request) {
        return ResponseEntity.ok(giftCardService.adjust(id, request));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<GiftCardTransactionDto>> transactions(@PathVariable UUID id) {
        return ResponseEntity.ok(giftCardService.transactions(id));
    }
}
