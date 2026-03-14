package com.inventory.system.service;

import com.inventory.system.common.entity.Coupon;
import com.inventory.system.common.entity.CouponStatus;
import com.inventory.system.common.entity.PricingRule;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.Promotion;
import com.inventory.system.common.entity.PromotionRedemption;
import com.inventory.system.common.entity.PromotionRedemptionStatus;
import com.inventory.system.common.entity.PromotionScope;
import com.inventory.system.common.entity.PromotionStatus;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
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
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CouponRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.PosTerminalRepository;
import com.inventory.system.repository.PricingRuleRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.PromotionRedemptionRepository;
import com.inventory.system.repository.PromotionRepository;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final PromotionRedemptionRepository promotionRedemptionRepository;
    private final WarehouseRepository warehouseRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final PricingEngineService pricingEngineService;

    @Override
    @Transactional
    public PromotionDto createPromotion(CreatePromotionRequest request) {
        promotionRepository.findByCode(request.getCode().trim()).ifPresent(existing -> {
            throw new BadRequestException("Promotion code already exists: " + request.getCode());
        });
        Promotion promotion = new Promotion();
        applyPromotionRequest(promotion, request);
        return mapPromotion(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionDto updatePromotion(UUID id, CreatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id));
        promotionRepository.findByCode(request.getCode().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Promotion code already exists: " + request.getCode());
                });
        applyPromotionRequest(promotion, request);
        return mapPromotion(promotionRepository.save(promotion));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDto getPromotion(UUID id) {
        return mapPromotion(promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotions(PromotionStatus status, SalesChannel salesChannel, Boolean couponRequired, String code) {
        return promotionRepository.findAll().stream()
                .filter(promotion -> status == null || promotion.getStatus() == status)
                .filter(promotion -> salesChannel == null || promotion.getSalesChannel() == salesChannel)
                .filter(promotion -> couponRequired == null || Objects.equals(promotion.getCouponRequired(), couponRequired))
                .filter(promotion -> code == null || code.isBlank() || promotion.getCode().toUpperCase().contains(code.trim().toUpperCase()))
                .sorted(Comparator.comparing(Promotion::getPriority, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Promotion::getName))
                .map(this::mapPromotion)
                .toList();
    }

    @Override
    @Transactional
    public CouponDto createCoupon(UUID promotionId, CreateCouponRequest request) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + promotionId));
        couponRepository.findByCode(request.getCode().trim()).ifPresent(existing -> {
            throw new BadRequestException("Coupon code already exists: " + request.getCode());
        });
        Coupon coupon = new Coupon();
        coupon.setPromotion(promotion);
        applyCouponRequest(coupon, request);
        return mapCoupon(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponDto updateCoupon(UUID couponId, CreateCouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + couponId));
        couponRepository.findByCode(request.getCode().trim())
                .filter(existing -> !existing.getId().equals(couponId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Coupon code already exists: " + request.getCode());
                });
        applyCouponRequest(coupon, request);
        return mapCoupon(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> getCoupons(UUID promotionId, CouponStatus status, String code) {
        return couponRepository.findAll().stream()
                .filter(coupon -> promotionId == null || coupon.getPromotion().getId().equals(promotionId))
                .filter(coupon -> status == null || coupon.getStatus() == status)
                .filter(coupon -> code == null || code.isBlank() || coupon.getCode().toUpperCase().contains(code.trim().toUpperCase()))
                .sorted(Comparator.comparing(Coupon::getCode))
                .map(this::mapCoupon)
                .toList();
    }

    @Override
    @Transactional
    public PricingRuleDto createPricingRule(CreatePricingRuleRequest request) {
        pricingRuleRepository.findByCode(request.getCode().trim()).ifPresent(existing -> {
            throw new BadRequestException("Pricing rule code already exists: " + request.getCode());
        });
        PricingRule rule = new PricingRule();
        applyPricingRuleRequest(rule, request);
        return mapPricingRule(pricingRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public PricingRuleDto updatePricingRule(UUID id, CreatePricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with ID: " + id));
        pricingRuleRepository.findByCode(request.getCode().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Pricing rule code already exists: " + request.getCode());
                });
        applyPricingRuleRequest(rule, request);
        return mapPricingRule(pricingRuleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDto> getPricingRules(PricingRuleStatus status, SalesChannel salesChannel, String code) {
        return pricingRuleRepository.findAll().stream()
                .filter(rule -> status == null || rule.getStatus() == status)
                .filter(rule -> salesChannel == null || rule.getSalesChannel() == salesChannel)
                .filter(rule -> code == null || code.isBlank() || rule.getCode().toUpperCase().contains(code.trim().toUpperCase()))
                .sorted(Comparator.comparing(PricingRule::getPriority, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(PricingRule::getName))
                .map(this::mapPricingRule)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPreviewDto previewPricing(PricingPreviewRequest request) {
        return pricingEngineService.toPreviewDto(pricingEngineService.evaluatePreview(request, true));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationDto validateCoupon(CouponValidationRequest request) {
        CouponValidationDto dto = new CouponValidationDto();
        try {
            PricingPreviewRequest previewRequest = new PricingPreviewRequest();
            previewRequest.setCustomerId(request.getCustomerId());
            previewRequest.setWarehouseId(request.getWarehouseId());
            previewRequest.setTerminalId(request.getTerminalId());
            previewRequest.setSalesChannel(request.getSalesChannel());
            previewRequest.setItems(request.getItems());
            previewRequest.setCouponCodes(List.of(request.getCouponCode()));

            Coupon coupon = couponRepository.findByCode(request.getCouponCode().trim())
                    .orElseThrow(() -> new BadRequestException("Coupon not found: " + request.getCouponCode()));

            dto.setCoupon(mapCoupon(coupon));
            dto.setPreview(pricingEngineService.toPreviewDto(pricingEngineService.evaluatePreview(previewRequest, true)));
            dto.setValid(true);
            dto.setMessage("Coupon is valid and applicable");
        } catch (Exception exception) {
            dto.setValid(false);
            dto.setMessage(exception.getMessage());
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionAnalyticsDto getAnalytics(LocalDateTime from, LocalDateTime to) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        List<PromotionRedemption> redemptions = promotionRedemptionRepository.findByRedeemedAtBetween(effectiveFrom, effectiveTo);

        PromotionAnalyticsDto dto = new PromotionAnalyticsDto();
        dto.setAppliedCount(redemptions.stream().filter(item -> item.getStatus() == PromotionRedemptionStatus.APPLIED).count());
        dto.setFlaggedCount(redemptions.stream().filter(item -> Boolean.TRUE.equals(item.getAbuseFlag()) || item.getStatus() == PromotionRedemptionStatus.FLAGGED).count());
        dto.setTotalDiscount(redemptions.stream()
                .filter(item -> item.getStatus() == PromotionRedemptionStatus.APPLIED || item.getStatus() == PromotionRedemptionStatus.FLAGGED)
                .map(PromotionRedemption::getDiscountAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), BigDecimal::add));

        Map<UUID, PromotionAnalyticsDto.PromotionUsageSummaryDto> summaries = new LinkedHashMap<>();
        for (PromotionRedemption redemption : redemptions) {
            PromotionAnalyticsDto.PromotionUsageSummaryDto summary = summaries.computeIfAbsent(redemption.getPromotion().getId(), key -> {
                PromotionAnalyticsDto.PromotionUsageSummaryDto item = new PromotionAnalyticsDto.PromotionUsageSummaryDto();
                item.setPromotionCode(redemption.getPromotion().getCode());
                item.setPromotionName(redemption.getPromotion().getName());
                item.setAppliedCount(0);
                item.setFlaggedCount(0);
                item.setTotalDiscount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
                return item;
            });
            if (redemption.getStatus() == PromotionRedemptionStatus.APPLIED || redemption.getStatus() == PromotionRedemptionStatus.FLAGGED) {
                summary.setAppliedCount(summary.getAppliedCount() + 1);
                summary.setTotalDiscount(summary.getTotalDiscount().add(redemption.getDiscountAmount()));
            }
            if (Boolean.TRUE.equals(redemption.getAbuseFlag()) || redemption.getStatus() == PromotionRedemptionStatus.FLAGGED) {
                summary.setFlaggedCount(summary.getFlaggedCount() + 1);
            }
        }
        dto.setPromotions(new ArrayList<>(summaries.values()));
        return dto;
    }

    private void applyPromotionRequest(Promotion promotion, CreatePromotionRequest request) {
        validatePromotionRequest(request);
        promotion.setName(request.getName().trim());
        promotion.setCode(request.getCode().trim().toUpperCase());
        promotion.setDescription(blankToNull(request.getDescription()));
        promotion.setStatus(request.getStatus());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setScope(request.getScope());
        promotion.setSalesChannel(request.getSalesChannel());
        promotion.setStartsAt(request.getStartsAt() != null ? request.getStartsAt() : LocalDateTime.now());
        promotion.setEndsAt(request.getEndsAt());
        promotion.setStackable(Boolean.TRUE.equals(request.getStackable()));
        promotion.setCouponRequired(Boolean.TRUE.equals(request.getCouponRequired()));
        promotion.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        promotion.setExclusionGroup(blankToNull(request.getExclusionGroup()));
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setMinQuantity(request.getMinQuantity());
        promotion.setBundleQuantity(request.getBundleQuantity());
        promotion.setBundlePrice(request.getBundlePrice());
        promotion.setBuyQuantity(request.getBuyQuantity());
        promotion.setGetQuantity(request.getGetQuantity());
        promotion.setUsageLimitTotal(request.getUsageLimitTotal());
        promotion.setUsageLimitPerCustomer(request.getUsageLimitPerCustomer());
        promotion.setCustomerCategory(request.getCustomerCategory());
        promotion.setWarehouse(request.getWarehouseId() != null ? warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new BadRequestException("Warehouse not found with ID: " + request.getWarehouseId())) : null);
        promotion.setTerminal(request.getTerminalId() != null ? posTerminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new BadRequestException("POS terminal not found with ID: " + request.getTerminalId())) : null);
        promotion.setCategory(request.getCategoryId() != null ? categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Category not found with ID: " + request.getCategoryId())) : null);
        promotion.setProductVariant(request.getProductVariantId() != null ? productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new BadRequestException("Product variant not found with ID: " + request.getProductVariantId())) : null);
    }

    private void validatePromotionRequest(CreatePromotionRequest request) {
        if (request.getStartsAt() != null && request.getEndsAt() != null && request.getEndsAt().isBefore(request.getStartsAt())) {
            throw new BadRequestException("Promotion end date cannot be before the start date");
        }
        if ((request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.BUNDLE
                || request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.BUY_X_GET_Y)
                && request.getScope() != PromotionScope.LINE) {
            throw new BadRequestException("Bundle and buy-X-get-Y promotions must use LINE scope");
        }
        if ((request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.FIXED_AMOUNT
                || request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.PERCENTAGE)
                && request.getDiscountValue() == null) {
            throw new BadRequestException("Discount value is required for fixed and percentage promotions");
        }
        if (request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.BUNDLE
                && (request.getBundleQuantity() == null || request.getBundlePrice() == null)) {
            throw new BadRequestException("Bundle promotions require bundle quantity and bundle price");
        }
        if (request.getDiscountType() == com.inventory.system.common.entity.PromotionDiscountType.BUY_X_GET_Y
                && (request.getBuyQuantity() == null || request.getGetQuantity() == null)) {
            throw new BadRequestException("Buy-X-get-Y promotions require buy and get quantities");
        }
    }

    private void applyCouponRequest(Coupon coupon, CreateCouponRequest request) {
        if (request.getValidTo() != null && request.getValidTo().isBefore(request.getValidFrom())) {
            throw new BadRequestException("Coupon valid-to cannot be before valid-from");
        }
        coupon.setCode(request.getCode().trim().toUpperCase());
        coupon.setStatus(request.getStatus());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        coupon.setMaxRedemptionsTotal(request.getMaxRedemptionsTotal());
        coupon.setMaxRedemptionsPerCustomer(request.getMaxRedemptionsPerCustomer());
        coupon.setNotes(blankToNull(request.getNotes()));
    }

    private void applyPricingRuleRequest(PricingRule rule, CreatePricingRuleRequest request) {
        if (request.getValidTo() != null && request.getValidTo().isBefore(request.getValidFrom())) {
            throw new BadRequestException("Pricing rule valid-to cannot be before valid-from");
        }
        rule.setName(request.getName().trim());
        rule.setCode(request.getCode().trim().toUpperCase());
        rule.setStatus(request.getStatus());
        rule.setAdjustmentType(request.getAdjustmentType());
        rule.setAdjustmentValue(request.getAdjustmentValue());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        rule.setSalesChannel(request.getSalesChannel());
        rule.setValidFrom(request.getValidFrom());
        rule.setValidTo(request.getValidTo());
        rule.setMinQuantity(request.getMinQuantity());
        rule.setCustomerCategory(request.getCustomerCategory());
        rule.setCustomer(request.getCustomerId() != null ? customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BadRequestException("Customer not found with ID: " + request.getCustomerId())) : null);
        rule.setWarehouse(request.getWarehouseId() != null ? warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new BadRequestException("Warehouse not found with ID: " + request.getWarehouseId())) : null);
        rule.setTerminal(request.getTerminalId() != null ? posTerminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new BadRequestException("POS terminal not found with ID: " + request.getTerminalId())) : null);
        rule.setCategory(request.getCategoryId() != null ? categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Category not found with ID: " + request.getCategoryId())) : null);
        rule.setProductVariant(request.getProductVariantId() != null ? productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new BadRequestException("Product variant not found with ID: " + request.getProductVariantId())) : null);
        rule.setNotes(blankToNull(request.getNotes()));
    }

    private PromotionDto mapPromotion(Promotion promotion) {
        PromotionDto dto = new PromotionDto();
        dto.setId(promotion.getId());
        dto.setName(promotion.getName());
        dto.setCode(promotion.getCode());
        dto.setDescription(promotion.getDescription());
        dto.setStatus(promotion.getStatus());
        dto.setDiscountType(promotion.getDiscountType());
        dto.setScope(promotion.getScope());
        dto.setSalesChannel(promotion.getSalesChannel());
        dto.setStartsAt(promotion.getStartsAt());
        dto.setEndsAt(promotion.getEndsAt());
        dto.setStackable(promotion.getStackable());
        dto.setCouponRequired(promotion.getCouponRequired());
        dto.setPriority(promotion.getPriority());
        dto.setExclusionGroup(promotion.getExclusionGroup());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        dto.setMinOrderAmount(promotion.getMinOrderAmount());
        dto.setMinQuantity(promotion.getMinQuantity());
        dto.setBundleQuantity(promotion.getBundleQuantity());
        dto.setBundlePrice(promotion.getBundlePrice());
        dto.setBuyQuantity(promotion.getBuyQuantity());
        dto.setGetQuantity(promotion.getGetQuantity());
        dto.setUsageLimitTotal(promotion.getUsageLimitTotal());
        dto.setUsageLimitPerCustomer(promotion.getUsageLimitPerCustomer());
        dto.setCustomerCategory(promotion.getCustomerCategory());
        dto.setWarehouseId(promotion.getWarehouse() != null ? promotion.getWarehouse().getId() : null);
        dto.setWarehouseName(promotion.getWarehouse() != null ? promotion.getWarehouse().getName() : null);
        dto.setTerminalId(promotion.getTerminal() != null ? promotion.getTerminal().getId() : null);
        dto.setTerminalName(promotion.getTerminal() != null ? promotion.getTerminal().getName() : null);
        dto.setCategoryId(promotion.getCategory() != null ? promotion.getCategory().getId() : null);
        dto.setCategoryName(promotion.getCategory() != null ? promotion.getCategory().getName() : null);
        dto.setProductVariantId(promotion.getProductVariant() != null ? promotion.getProductVariant().getId() : null);
        dto.setProductVariantSku(promotion.getProductVariant() != null ? promotion.getProductVariant().getSku() : null);
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setUpdatedAt(promotion.getUpdatedAt());
        return dto;
    }

    private CouponDto mapCoupon(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setId(coupon.getId());
        dto.setPromotionId(coupon.getPromotion().getId());
        dto.setPromotionName(coupon.getPromotion().getName());
        dto.setPromotionCode(coupon.getPromotion().getCode());
        dto.setCode(coupon.getCode());
        dto.setStatus(coupon.getStatus());
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidTo(coupon.getValidTo());
        dto.setMaxRedemptionsTotal(coupon.getMaxRedemptionsTotal());
        dto.setMaxRedemptionsPerCustomer(coupon.getMaxRedemptionsPerCustomer());
        dto.setRedeemedCount(coupon.getRedeemedCount());
        dto.setNotes(coupon.getNotes());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setUpdatedAt(coupon.getUpdatedAt());
        return dto;
    }

    private PricingRuleDto mapPricingRule(PricingRule rule) {
        PricingRuleDto dto = new PricingRuleDto();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setCode(rule.getCode());
        dto.setStatus(rule.getStatus());
        dto.setAdjustmentType(rule.getAdjustmentType());
        dto.setAdjustmentValue(rule.getAdjustmentValue());
        dto.setPriority(rule.getPriority());
        dto.setSalesChannel(rule.getSalesChannel());
        dto.setValidFrom(rule.getValidFrom());
        dto.setValidTo(rule.getValidTo());
        dto.setMinQuantity(rule.getMinQuantity());
        dto.setCustomerCategory(rule.getCustomerCategory());
        dto.setCustomerId(rule.getCustomer() != null ? rule.getCustomer().getId() : null);
        dto.setCustomerName(rule.getCustomer() != null ? rule.getCustomer().getName() : null);
        dto.setWarehouseId(rule.getWarehouse() != null ? rule.getWarehouse().getId() : null);
        dto.setWarehouseName(rule.getWarehouse() != null ? rule.getWarehouse().getName() : null);
        dto.setTerminalId(rule.getTerminal() != null ? rule.getTerminal().getId() : null);
        dto.setTerminalName(rule.getTerminal() != null ? rule.getTerminal().getName() : null);
        dto.setCategoryId(rule.getCategory() != null ? rule.getCategory().getId() : null);
        dto.setCategoryName(rule.getCategory() != null ? rule.getCategory().getName() : null);
        dto.setProductVariantId(rule.getProductVariant() != null ? rule.getProductVariant().getId() : null);
        dto.setProductVariantSku(rule.getProductVariant() != null ? rule.getProductVariant().getSku() : null);
        dto.setNotes(rule.getNotes());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}