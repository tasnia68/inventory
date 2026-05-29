package com.inventory.system.controller;

import com.inventory.system.common.entity.DiscountChannel;
import com.inventory.system.payload.CreateDiscountCodeRequest;
import com.inventory.system.payload.CreateDiscountRequest;
import com.inventory.system.payload.DiscountAnalyticsDto;
import com.inventory.system.payload.DiscountCodeDto;
import com.inventory.system.payload.DiscountDto;
import com.inventory.system.payload.PricingPreviewRequest;
import com.inventory.system.payload.PricingPreviewResponse;
import com.inventory.system.service.DiscountService;
import com.inventory.system.service.PricingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;
    private final PricingEngineService pricingEngineService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DiscountDto> create(@RequestBody CreateDiscountRequest request) {
        return ResponseEntity.ok(discountService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DiscountDto> update(@PathVariable UUID id, @RequestBody CreateDiscountRequest request) {
        return ResponseEntity.ok(discountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        discountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<DiscountDto>> list() {
        return ResponseEntity.ok(discountService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(discountService.get(id));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DiscountDto>> available(@RequestParam(required = false) DiscountChannel channel) {
        return ResponseEntity.ok(discountService.listAvailable(channel));
    }

    @PostMapping("/{discountId}/codes")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DiscountCodeDto> createCode(@PathVariable UUID discountId,
                                                      @RequestBody CreateDiscountCodeRequest request) {
        return ResponseEntity.ok(discountService.createCode(discountId, request));
    }

    @PutMapping("/codes/{codeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DiscountCodeDto> updateCode(@PathVariable UUID codeId,
                                                      @RequestBody CreateDiscountCodeRequest request) {
        return ResponseEntity.ok(discountService.updateCode(codeId, request));
    }

    @DeleteMapping("/codes/{codeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> deleteCode(@PathVariable UUID codeId) {
        discountService.deleteCode(codeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/codes")
    public ResponseEntity<List<DiscountCodeDto>> listCodes(@RequestParam(required = false) UUID discountId) {
        return ResponseEntity.ok(discountService.listCodes(discountId));
    }

    @PostMapping("/preview")
    public ResponseEntity<PricingPreviewResponse> preview(@RequestBody PricingPreviewRequest request) {
        return ResponseEntity.ok(pricingEngineService.preview(request));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DiscountAnalyticsDto> analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(discountService.analytics(from, to));
    }
}
