package com.inventory.system.controller;

import com.inventory.system.common.entity.CouponStatus;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.PromotionStatus;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CouponDto;
import com.inventory.system.payload.CouponValidationDto;
import com.inventory.system.payload.CouponValidationRequest;
import com.inventory.system.payload.CreateCouponRequest;
import com.inventory.system.payload.CreatePricingRuleRequest;
import com.inventory.system.payload.CreatePromotionRequest;
import com.inventory.system.payload.PricingPreviewDto;
import com.inventory.system.payload.PricingPreviewRequest;
import com.inventory.system.payload.PricingRuleDto;
import com.inventory.system.payload.PromotionAnalyticsDto;
import com.inventory.system.payload.PromotionDto;
import com.inventory.system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        return new ResponseEntity<>(ApiResponse.success(promotionService.createPromotion(request), "Promotion created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(@PathVariable UUID id, @Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.updatePromotion(id, request), "Promotion updated successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotion(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getPromotion(id), "Promotion retrieved successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getPromotions(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) SalesChannel salesChannel,
            @RequestParam(required = false) Boolean couponRequired,
            @RequestParam(required = false) String code) {
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.getPromotions(status, salesChannel, couponRequired, code),
                "Promotions retrieved successfully"
        ));
    }

    @PostMapping("/{promotionId}/coupons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CouponDto>> createCoupon(@PathVariable UUID promotionId, @Valid @RequestBody CreateCouponRequest request) {
        return new ResponseEntity<>(ApiResponse.success(promotionService.createCoupon(promotionId, request), "Coupon created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/coupons/{couponId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CouponDto>> updateCoupon(@PathVariable UUID couponId, @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.updateCoupon(couponId, request), "Coupon updated successfully"));
    }

    @GetMapping("/coupons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CouponDto>>> getCoupons(
            @RequestParam(required = false) UUID promotionId,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) String code) {
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.getCoupons(promotionId, status, code),
                "Coupons retrieved successfully"
        ));
    }

    @PostMapping("/pricing-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PricingRuleDto>> createPricingRule(@Valid @RequestBody CreatePricingRuleRequest request) {
        return new ResponseEntity<>(ApiResponse.success(promotionService.createPricingRule(request), "Pricing rule created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/pricing-rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PricingRuleDto>> updatePricingRule(@PathVariable UUID id, @Valid @RequestBody CreatePricingRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.updatePricingRule(id, request), "Pricing rule updated successfully"));
    }

    @GetMapping("/pricing-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PricingRuleDto>>> getPricingRules(
            @RequestParam(required = false) PricingRuleStatus status,
            @RequestParam(required = false) SalesChannel salesChannel,
            @RequestParam(required = false) String code) {
        return ResponseEntity.ok(ApiResponse.success(
                promotionService.getPricingRules(status, salesChannel, code),
                "Pricing rules retrieved successfully"
        ));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PricingPreviewDto>> previewPricing(@Valid @RequestBody PricingPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.previewPricing(request), "Pricing preview generated successfully"));
    }

    @PostMapping("/coupons/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<CouponValidationDto>> validateCoupon(@Valid @RequestBody CouponValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.validateCoupon(request), "Coupon validation completed successfully"));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PromotionAnalyticsDto>> getAnalytics(
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(promotionService.getAnalytics(from, to), "Promotion analytics retrieved successfully"));
    }
}