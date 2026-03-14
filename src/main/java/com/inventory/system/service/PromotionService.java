package com.inventory.system.service;

import com.inventory.system.common.entity.CouponStatus;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.PromotionStatus;
import com.inventory.system.common.entity.SalesChannel;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PromotionService {
    PromotionDto createPromotion(CreatePromotionRequest request);
    PromotionDto updatePromotion(UUID id, CreatePromotionRequest request);
    PromotionDto getPromotion(UUID id);
    List<PromotionDto> getPromotions(PromotionStatus status, SalesChannel salesChannel, Boolean couponRequired, String code);

    CouponDto createCoupon(UUID promotionId, CreateCouponRequest request);
    CouponDto updateCoupon(UUID couponId, CreateCouponRequest request);
    List<CouponDto> getCoupons(UUID promotionId, CouponStatus status, String code);

    PricingRuleDto createPricingRule(CreatePricingRuleRequest request);
    PricingRuleDto updatePricingRule(UUID id, CreatePricingRuleRequest request);
    List<PricingRuleDto> getPricingRules(PricingRuleStatus status, SalesChannel salesChannel, String code);

    PricingPreviewDto previewPricing(PricingPreviewRequest request);
    CouponValidationDto validateCoupon(CouponValidationRequest request);
    PromotionAnalyticsDto getAnalytics(LocalDateTime from, LocalDateTime to);
}