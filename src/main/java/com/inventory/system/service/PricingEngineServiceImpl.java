package com.inventory.system.service;

import com.inventory.system.common.entity.Category;
import com.inventory.system.common.entity.Coupon;
import com.inventory.system.common.entity.CouponStatus;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.CustomerPriceList;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosTerminal;
import com.inventory.system.common.entity.PricingRule;
import com.inventory.system.common.entity.PricingRuleAdjustmentType;
import com.inventory.system.common.entity.PricingRuleStatus;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.Promotion;
import com.inventory.system.common.entity.PromotionDiscountType;
import com.inventory.system.common.entity.PromotionRedemption;
import com.inventory.system.common.entity.PromotionRedemptionStatus;
import com.inventory.system.common.entity.PromotionScope;
import com.inventory.system.common.entity.PromotionStatus;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.payload.AppliedPromotionDto;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.PosSaleItemRequest;
import com.inventory.system.payload.PricingPreviewDto;
import com.inventory.system.payload.PricingPreviewItemRequest;
import com.inventory.system.payload.PricingPreviewLineDto;
import com.inventory.system.payload.PricingPreviewRequest;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CouponRepository;
import com.inventory.system.repository.CustomerPriceListRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingEngineServiceImpl implements PricingEngineService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final PosTerminalRepository posTerminalRepository;
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final PromotionRedemptionRepository promotionRedemptionRepository;
    private final CustomerPriceListRepository customerPriceListRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PricingEvaluation evaluateSalesOrder(Customer customer, Warehouse warehouse, SalesOrderRequest request) {
        return evaluate(
                customer,
                warehouse,
                null,
                SalesChannel.SALES_ORDER,
                request.getCouponCodes(),
                ZERO,
                request.getItems().stream()
                        .map(item -> new EvaluationItem(item.getProductVariantId(), item.getQuantity(), item.getUnitPrice(), ZERO))
                        .toList(),
                true
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PricingEvaluation evaluatePosSale(Customer customer, Warehouse warehouse, PosTerminal terminal, CreatePosSaleRequest request, boolean allowManualDiscount) {
        return evaluate(
                customer,
                warehouse,
                terminal,
                SalesChannel.POS,
                request.getCouponCodes(),
                scale(request.getDiscountAmount()),
                request.getItems().stream()
                        .map(item -> new EvaluationItem(item.getProductVariantId(), item.getQuantity(), item.getUnitPrice(), scale(item.getLineDiscount())))
                        .toList(),
                allowManualDiscount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PricingEvaluation evaluatePreview(PricingPreviewRequest request, boolean allowManualDiscount) {
        Customer customer = request.getCustomerId() != null ? customerRepository.findById(request.getCustomerId()).orElse(null) : null;
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new BadRequestException("Warehouse not found with ID: " + request.getWarehouseId()));
        PosTerminal terminal = request.getTerminalId() != null
                ? posTerminalRepository.findById(request.getTerminalId()).orElseThrow(() -> new BadRequestException("POS terminal not found with ID: " + request.getTerminalId()))
                : null;

        return evaluate(
                customer,
                warehouse,
                terminal,
                request.getSalesChannel(),
                request.getCouponCodes(),
                scale(request.getManualDiscountAmount()),
                request.getItems().stream()
                        .map(item -> new EvaluationItem(item.getProductVariantId(), item.getQuantity(), item.getUnitPrice(), scale(item.getManualLineDiscount())))
                        .toList(),
                allowManualDiscount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPreviewDto toPreviewDto(PricingEvaluation evaluation) {
        PricingPreviewDto dto = new PricingPreviewDto();
        dto.setBaseSubtotal(evaluation.getBaseSubtotal());
        dto.setLineDiscountTotal(evaluation.getLineDiscountTotal());
        dto.setOrderDiscountTotal(evaluation.getOrderDiscountTotal());
        dto.setManualDiscountTotal(evaluation.getManualDiscountTotal());
        dto.setTotalDiscount(evaluation.getTotalDiscount());
        dto.setNetSubtotal(evaluation.getNetSubtotal());
        dto.setAppliedCouponCodes(new ArrayList<>(evaluation.getAppliedCouponCodes()));
        dto.setAppliedPromotions(evaluation.getAppliedPromotions().stream().map(this::mapAppliedPromotion).toList());
        dto.setLines(evaluation.getLines().stream().map(this::mapLine).toList());
        return dto;
    }

    @Override
    @Transactional
    public void recordRedemptions(PricingEvaluation evaluation,
                                  SalesOrder salesOrder,
                                  PosSale posSale,
                                  Customer customer,
                                  SalesChannel salesChannel,
                                  String referenceNumber) {
        if (evaluation.getAppliedPromotions().isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (AppliedPromotionEvaluation appliedPromotion : evaluation.getAppliedPromotions()) {
            PromotionRedemption redemption = new PromotionRedemption();
            redemption.setPromotion(appliedPromotion.getPromotion());
            redemption.setCoupon(appliedPromotion.getCoupon());
            redemption.setCustomer(customer);
            redemption.setSalesOrder(salesOrder);
            redemption.setPosSale(posSale);
            redemption.setSalesChannel(salesChannel);
            redemption.setStatus(PromotionRedemptionStatus.APPLIED);
            redemption.setDiscountAmount(scale(appliedPromotion.getDiscountAmount()));
            redemption.setOrderSubtotal(scale(evaluation.getBaseSubtotal()));
            redemption.setReferenceNumber(referenceNumber);
            redemption.setRedeemedAt(now);

            if (appliedPromotion.getCoupon() != null && customer != null) {
                long recentCount = promotionRedemptionRepository.countByCouponIdAndCustomerIdAndRedeemedAtAfter(
                        appliedPromotion.getCoupon().getId(),
                        customer.getId(),
                        now.minusHours(24)
                );
                if (recentCount >= 3) {
                    redemption.setAbuseFlag(true);
                    redemption.setStatus(PromotionRedemptionStatus.FLAGGED);
                    redemption.setAbuseReason("High-frequency coupon redemption pattern detected in the last 24 hours");
                }
            }

            promotionRedemptionRepository.save(redemption);

            if (appliedPromotion.getCoupon() != null) {
                Coupon coupon = appliedPromotion.getCoupon();
                coupon.setRedeemedCount((coupon.getRedeemedCount() == null ? 0 : coupon.getRedeemedCount()) + 1);
                couponRepository.save(coupon);
            }
        }
    }

    private PricingEvaluation evaluate(Customer customer,
                                       Warehouse warehouse,
                                       PosTerminal terminal,
                                       SalesChannel salesChannel,
                                       List<String> couponCodes,
                                       BigDecimal manualOrderDiscount,
                                       List<EvaluationItem> items,
                                       boolean allowManualDiscount) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one item is required for pricing evaluation");
        }

        if (!allowManualDiscount && manualOrderDiscount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Manual order discount override is not permitted for the current user");
        }

        LocalDateTime now = LocalDateTime.now();
        Map<UUID, ProductVariant> variants = loadVariants(items);
        Map<UUID, CustomerPriceList> priceLists = loadPriceLists(customer, variants.keySet(), now.toLocalDate());
        List<PricingRule> pricingRules = pricingRuleRepository.findAll();
        List<Promotion> promotions = promotionRepository.findAll();
        Map<String, Coupon> providedCoupons = resolveCoupons(couponCodes, customer, now);

        PricingEvaluation evaluation = new PricingEvaluation();
        List<AppliedPromotionEvaluation> appliedPromotions = new ArrayList<>();

        for (EvaluationItem item : items) {
            if (!allowManualDiscount && item.manualLineDiscount.compareTo(BigDecimal.ZERO) > 0) {
                throw new BadRequestException("Manual line discount override is not permitted for the current user");
            }

            ProductVariant variant = variants.get(item.productVariantId);
            BigDecimal quantity = scale(item.quantity);
            BigDecimal baseUnitPrice = resolveBaseUnitPrice(item.requestedUnitPrice, customer, variant, priceLists.get(variant.getId()));
            BigDecimal baseLineAmount = scale(baseUnitPrice.multiply(quantity));
            evaluation.setBaseSubtotal(scale(evaluation.getBaseSubtotal().add(baseLineAmount)));

            PricingRule pricingRule = selectBestPricingRule(pricingRules, customer, warehouse, terminal, salesChannel, variant, quantity, now);
            BigDecimal pricedUnit = applyPricingRule(baseUnitPrice, pricingRule);
            BigDecimal pricingRuleDiscount = scale(baseUnitPrice.subtract(pricedUnit).max(BigDecimal.ZERO).multiply(quantity));

            LinePromotionSelection lineSelection = applyLinePromotions(promotions, providedCoupons, customer, warehouse, terminal, salesChannel, variant, quantity, pricedUnit, now);

            BigDecimal manualLineDiscount = scale(item.manualLineDiscount);
            BigDecimal lineDiscount = pricingRuleDiscount.add(lineSelection.totalDiscount).add(manualLineDiscount);
            if (lineDiscount.compareTo(baseLineAmount) > 0) {
                lineDiscount = baseLineAmount;
            }

            BigDecimal lineTotal = scale(baseLineAmount.subtract(lineDiscount));
            BigDecimal finalUnitPrice = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? scale(lineTotal.divide(quantity, 6, RoundingMode.HALF_UP))
                    : ZERO;

            PricingEvaluationLine line = new PricingEvaluationLine();
            line.setProductVariant(variant);
            line.setQuantity(quantity);
            line.setBaseUnitPrice(baseUnitPrice);
            line.setBaseLineAmount(baseLineAmount);
            line.setLineDiscountAmount(scale(lineDiscount));
            line.setLineTotalAmount(lineTotal);
            line.setFinalUnitPrice(finalUnitPrice);
            line.setManualLineDiscount(manualLineDiscount);
            line.setAppliedPromotionCodes(lineSelection.codes);
            evaluation.getLines().add(line);

            evaluation.setLineDiscountTotal(scale(evaluation.getLineDiscountTotal().add(lineDiscount)));
            evaluation.setManualDiscountTotal(scale(evaluation.getManualDiscountTotal().add(manualLineDiscount)));

            mergeAppliedPromotions(appliedPromotions, lineSelection.appliedPromotions);
        }

        BigDecimal orderBaseAfterLine = scale(evaluation.getBaseSubtotal().subtract(evaluation.getLineDiscountTotal()));
        OrderPromotionSelection orderSelection = applyOrderPromotions(promotions, providedCoupons, customer, warehouse, terminal, salesChannel, orderBaseAfterLine, now);
        mergeAppliedPromotions(appliedPromotions, orderSelection.appliedPromotions);

        BigDecimal orderDiscountTotal = orderSelection.totalDiscount;
        BigDecimal totalDiscount = evaluation.getLineDiscountTotal().add(orderDiscountTotal).add(manualOrderDiscount);
        if (totalDiscount.compareTo(evaluation.getBaseSubtotal()) > 0) {
            totalDiscount = evaluation.getBaseSubtotal();
        }

        evaluation.setOrderDiscountTotal(scale(orderDiscountTotal));
        evaluation.setManualDiscountTotal(scale(evaluation.getManualDiscountTotal().add(manualOrderDiscount)));
        evaluation.setTotalDiscount(scale(totalDiscount));
        evaluation.setNetSubtotal(scale(evaluation.getBaseSubtotal().subtract(totalDiscount)));
        evaluation.setAppliedPromotions(appliedPromotions);
        evaluation.setAppliedCouponCodes(appliedPromotions.stream()
                .map(AppliedPromotionEvaluation::getCoupon)
                .filter(Objects::nonNull)
                .map(Coupon::getCode)
                .distinct()
                .toList());

        return evaluation;
    }

    private Map<UUID, ProductVariant> loadVariants(List<EvaluationItem> items) {
        Set<UUID> variantIds = items.stream().map(item -> item.productVariantId).collect(Collectors.toSet());
        Map<UUID, ProductVariant> variants = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
        if (variants.size() != variantIds.size()) {
            throw new BadRequestException("One or more pricing items reference missing product variants");
        }
        return variants;
    }

    private Map<UUID, CustomerPriceList> loadPriceLists(Customer customer, Set<UUID> variantIds, LocalDate businessDate) {
        if (customer == null) {
            return Map.of();
        }

        return customerPriceListRepository.findByCustomerId(customer.getId()).stream()
                .filter(priceList -> variantIds.contains(priceList.getProductVariant().getId()))
                .filter(priceList -> isPriceListActive(priceList, businessDate))
                .collect(Collectors.toMap(
                        priceList -> priceList.getProductVariant().getId(),
                        priceList -> priceList,
                        (left, right) -> left.getCreatedAt().isAfter(right.getCreatedAt()) ? left : right
                ));
    }

    private boolean isPriceListActive(CustomerPriceList priceList, LocalDate businessDate) {
        return (priceList.getEffectiveFrom() == null || !businessDate.isBefore(priceList.getEffectiveFrom()))
                && (priceList.getEffectiveTo() == null || !businessDate.isAfter(priceList.getEffectiveTo()));
    }

    private Map<String, Coupon> resolveCoupons(List<String> couponCodes, Customer customer, LocalDateTime now) {
        if (couponCodes == null || couponCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, Coupon> coupons = new LinkedHashMap<>();
        for (String couponCode : couponCodes.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty()).toList()) {
            Coupon coupon = couponRepository.findByCode(couponCode)
                    .orElseThrow(() -> new BadRequestException("Coupon not found: " + couponCode));
            validateCouponEligibility(coupon, customer, now);
            coupons.put(coupon.getCode(), coupon);
        }
        return coupons;
    }

    private void validateCouponEligibility(Coupon coupon, Customer customer, LocalDateTime now) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BadRequestException("Coupon is not active: " + coupon.getCode());
        }
        if (now.isBefore(coupon.getValidFrom()) || (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo()))) {
            throw new BadRequestException("Coupon is outside its validity window: " + coupon.getCode());
        }

        long totalRedemptions = promotionRedemptionRepository.countByCouponIdAndStatus(coupon.getId(), PromotionRedemptionStatus.APPLIED);
        if (coupon.getMaxRedemptionsTotal() != null && totalRedemptions >= coupon.getMaxRedemptionsTotal()) {
            throw new BadRequestException("Coupon redemption limit reached: " + coupon.getCode());
        }
        if (customer != null && coupon.getMaxRedemptionsPerCustomer() != null) {
            long customerRedemptions = promotionRedemptionRepository.countByCouponIdAndCustomerIdAndStatus(
                    coupon.getId(),
                    customer.getId(),
                    PromotionRedemptionStatus.APPLIED
            );
            if (customerRedemptions >= coupon.getMaxRedemptionsPerCustomer()) {
                throw new BadRequestException("Coupon redemption limit reached for customer: " + coupon.getCode());
            }
        }
    }

    private BigDecimal resolveBaseUnitPrice(BigDecimal requestedUnitPrice,
                                            Customer customer,
                                            ProductVariant variant,
                                            CustomerPriceList customerPriceList) {
        if (requestedUnitPrice != null) {
            return scale(requestedUnitPrice);
        }
        if (customer != null && customerPriceList != null) {
            return scale(customerPriceList.getPrice());
        }
        return scale(variant.getPrice());
    }

    private PricingRule selectBestPricingRule(List<PricingRule> pricingRules,
                                              Customer customer,
                                              Warehouse warehouse,
                                              PosTerminal terminal,
                                              SalesChannel salesChannel,
                                              ProductVariant variant,
                                              BigDecimal quantity,
                                              LocalDateTime now) {
        return pricingRules.stream()
                .filter(rule -> rule.getStatus() == PricingRuleStatus.ACTIVE)
                .filter(rule -> !now.isBefore(rule.getValidFrom()))
                .filter(rule -> rule.getValidTo() == null || !now.isAfter(rule.getValidTo()))
                .filter(rule -> rule.getSalesChannel() == null || rule.getSalesChannel() == salesChannel)
                .filter(rule -> rule.getCustomer() == null || (customer != null && rule.getCustomer().getId().equals(customer.getId())))
                .filter(rule -> rule.getCustomerCategory() == null || (customer != null && rule.getCustomerCategory() == customer.getCategory()))
                .filter(rule -> rule.getWarehouse() == null || rule.getWarehouse().getId().equals(warehouse.getId()))
                .filter(rule -> rule.getTerminal() == null || (terminal != null && rule.getTerminal().getId().equals(terminal.getId())))
                .filter(rule -> rule.getCategory() == null || belongsToCategory(variant, rule.getCategory()))
                .filter(rule -> rule.getProductVariant() == null || rule.getProductVariant().getId().equals(variant.getId()))
                .filter(rule -> rule.getMinQuantity() == null || quantity.compareTo(rule.getMinQuantity()) >= 0)
                .sorted(Comparator
                        .comparing(PricingRule::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(this::specificityScore, Comparator.reverseOrder())
                        .thenComparing(PricingRule::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal applyPricingRule(BigDecimal baseUnitPrice, PricingRule pricingRule) {
        if (pricingRule == null) {
            return baseUnitPrice;
        }

        return switch (pricingRule.getAdjustmentType()) {
            case FIXED_PRICE -> scale(pricingRule.getAdjustmentValue());
            case FIXED_AMOUNT_OFF -> scale(baseUnitPrice.subtract(pricingRule.getAdjustmentValue()).max(BigDecimal.ZERO));
            case PERCENTAGE_OFF -> {
                BigDecimal discount = baseUnitPrice.multiply(scale(pricingRule.getAdjustmentValue())).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                yield scale(baseUnitPrice.subtract(discount).max(BigDecimal.ZERO));
            }
        };
    }

    private LinePromotionSelection applyLinePromotions(List<Promotion> promotions,
                                                       Map<String, Coupon> providedCoupons,
                                                       Customer customer,
                                                       Warehouse warehouse,
                                                       PosTerminal terminal,
                                                       SalesChannel salesChannel,
                                                       ProductVariant variant,
                                                       BigDecimal quantity,
                                                       BigDecimal adjustedUnitPrice,
                                                       LocalDateTime now) {
        BigDecimal baseAmount = scale(adjustedUnitPrice.multiply(quantity));
        List<PromotionCandidate> candidates = promotions.stream()
                .filter(promotion -> isPromotionApplicable(promotion, providedCoupons, customer, warehouse, terminal, salesChannel, variant, quantity, now, baseAmount))
                .map(promotion -> createLineCandidate(promotion, providedCoupons, variant, quantity, adjustedUnitPrice, baseAmount))
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.discountAmount.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator
                        .comparing((PromotionCandidate candidate) -> candidate.promotion.getPriority(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> candidate.discountAmount, Comparator.reverseOrder()))
                .toList();

        return selectPromotionCandidates(candidates);
    }

    private OrderPromotionSelection applyOrderPromotions(List<Promotion> promotions,
                                                         Map<String, Coupon> providedCoupons,
                                                         Customer customer,
                                                         Warehouse warehouse,
                                                         PosTerminal terminal,
                                                         SalesChannel salesChannel,
                                                         BigDecimal orderAmount,
                                                         LocalDateTime now) {
        List<PromotionCandidate> candidates = promotions.stream()
                .filter(promotion -> promotion.getScope() == PromotionScope.ORDER)
                .filter(promotion -> promotion.getDiscountType() == PromotionDiscountType.FIXED_AMOUNT || promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE)
                .filter(promotion -> isPromotionApplicable(promotion, providedCoupons, customer, warehouse, terminal, salesChannel, null, BigDecimal.ONE, now, orderAmount))
                .map(promotion -> createOrderCandidate(promotion, providedCoupons, orderAmount))
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.discountAmount.compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator
                        .comparing((PromotionCandidate candidate) -> candidate.promotion.getPriority(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(candidate -> candidate.discountAmount, Comparator.reverseOrder()))
                .toList();

        LinePromotionSelection selection = selectPromotionCandidates(candidates);
        OrderPromotionSelection orderSelection = new OrderPromotionSelection();
        orderSelection.totalDiscount = selection.totalDiscount;
        orderSelection.appliedPromotions = selection.appliedPromotions;
        return orderSelection;
    }

    private boolean isPromotionApplicable(Promotion promotion,
                                          Map<String, Coupon> providedCoupons,
                                          Customer customer,
                                          Warehouse warehouse,
                                          PosTerminal terminal,
                                          SalesChannel salesChannel,
                                          ProductVariant variant,
                                          BigDecimal quantity,
                                          LocalDateTime now,
                                          BigDecimal amountContext) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            return false;
        }
        if (now.isBefore(promotion.getStartsAt()) || (promotion.getEndsAt() != null && now.isAfter(promotion.getEndsAt()))) {
            return false;
        }
        if (promotion.getSalesChannel() != null && promotion.getSalesChannel() != salesChannel) {
            return false;
        }
        if (promotion.getWarehouse() != null && !promotion.getWarehouse().getId().equals(warehouse.getId())) {
            return false;
        }
        if (promotion.getTerminal() != null && (terminal == null || !promotion.getTerminal().getId().equals(terminal.getId()))) {
            return false;
        }
        if (promotion.getCustomerCategory() != null && (customer == null || promotion.getCustomerCategory() != customer.getCategory())) {
            return false;
        }
        if (promotion.getCategory() != null && (variant == null || !belongsToCategory(variant, promotion.getCategory()))) {
            return false;
        }
        if (promotion.getProductVariant() != null && (variant == null || !promotion.getProductVariant().getId().equals(variant.getId()))) {
            return false;
        }
        if (promotion.getMinQuantity() != null && quantity.compareTo(promotion.getMinQuantity()) < 0) {
            return false;
        }
        if (promotion.getMinOrderAmount() != null && amountContext.compareTo(promotion.getMinOrderAmount()) < 0) {
            return false;
        }
        if (Boolean.TRUE.equals(promotion.getCouponRequired()) && providedCoupons.values().stream().noneMatch(coupon -> coupon.getPromotion().getId().equals(promotion.getId()))) {
            return false;
        }

        if (promotion.getUsageLimitTotal() != null) {
            long totalUsage = promotionRedemptionRepository.countByPromotionIdAndStatus(promotion.getId(), PromotionRedemptionStatus.APPLIED);
            if (totalUsage >= promotion.getUsageLimitTotal()) {
                return false;
            }
        }
        if (customer != null && promotion.getUsageLimitPerCustomer() != null) {
            long customerUsage = promotionRedemptionRepository.countByPromotionIdAndCustomerIdAndStatus(
                    promotion.getId(),
                    customer.getId(),
                    PromotionRedemptionStatus.APPLIED
            );
            if (customerUsage >= promotion.getUsageLimitPerCustomer()) {
                return false;
            }
        }
        return true;
    }

    private PromotionCandidate createLineCandidate(Promotion promotion,
                                                   Map<String, Coupon> providedCoupons,
                                                   ProductVariant variant,
                                                   BigDecimal quantity,
                                                   BigDecimal adjustedUnitPrice,
                                                   BigDecimal baseAmount) {
        BigDecimal discountAmount = switch (promotion.getDiscountType()) {
            case FIXED_AMOUNT -> scale(promotion.getDiscountValue().multiply(quantity));
            case PERCENTAGE -> percentageDiscount(baseAmount, promotion.getDiscountValue(), promotion.getMaxDiscountAmount());
            case BUNDLE -> {
                if (promotion.getBundleQuantity() == null || promotion.getBundlePrice() == null || promotion.getBundleQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    yield null;
                }
                BigDecimal bundles = quantity.divideToIntegralValue(promotion.getBundleQuantity());
                if (bundles.compareTo(BigDecimal.ZERO) <= 0) {
                    yield null;
                }
                BigDecimal regularBundleAmount = adjustedUnitPrice.multiply(promotion.getBundleQuantity()).multiply(bundles);
                BigDecimal discountedBundleAmount = promotion.getBundlePrice().multiply(bundles);
                yield scale(regularBundleAmount.subtract(discountedBundleAmount).max(BigDecimal.ZERO));
            }
            case BUY_X_GET_Y -> {
                if (promotion.getBuyQuantity() == null || promotion.getGetQuantity() == null || promotion.getBuyQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    yield null;
                }
                BigDecimal cycleQuantity = promotion.getBuyQuantity().add(promotion.getGetQuantity());
                BigDecimal cycles = quantity.divideToIntegralValue(cycleQuantity);
                if (cycles.compareTo(BigDecimal.ZERO) <= 0) {
                    yield null;
                }
                BigDecimal freeQty = cycles.multiply(promotion.getGetQuantity());
                yield scale(adjustedUnitPrice.multiply(freeQty));
            }
        };

        if (discountAmount == null) {
            return null;
        }

        if (promotion.getMaxDiscountAmount() != null && discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discountAmount = scale(promotion.getMaxDiscountAmount());
        }

        PromotionCandidate candidate = new PromotionCandidate();
        candidate.promotion = promotion;
        candidate.coupon = couponForPromotion(promotion, providedCoupons);
        candidate.discountAmount = scale(discountAmount.min(baseAmount));
        return candidate;
    }

    private PromotionCandidate createOrderCandidate(Promotion promotion,
                                                    Map<String, Coupon> providedCoupons,
                                                    BigDecimal orderAmount) {
        BigDecimal discountAmount = switch (promotion.getDiscountType()) {
            case FIXED_AMOUNT -> scale(promotion.getDiscountValue());
            case PERCENTAGE -> percentageDiscount(orderAmount, promotion.getDiscountValue(), promotion.getMaxDiscountAmount());
            default -> ZERO;
        };

        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        PromotionCandidate candidate = new PromotionCandidate();
        candidate.promotion = promotion;
        candidate.coupon = couponForPromotion(promotion, providedCoupons);
        candidate.discountAmount = scale(discountAmount.min(orderAmount));
        return candidate;
    }

    private LinePromotionSelection selectPromotionCandidates(List<PromotionCandidate> candidates) {
        LinePromotionSelection selection = new LinePromotionSelection();
        Set<String> exclusionGroups = new HashSet<>();
        boolean lockedByNonStackable = false;

        for (PromotionCandidate candidate : candidates) {
            String exclusionGroup = candidate.promotion.getExclusionGroup();
            if (lockedByNonStackable) {
                continue;
            }
            if (exclusionGroup != null && exclusionGroups.contains(exclusionGroup)) {
                continue;
            }

            AppliedPromotionEvaluation applied = new AppliedPromotionEvaluation();
            applied.setPromotion(candidate.promotion);
            applied.setCoupon(candidate.coupon);
            applied.setDiscountAmount(candidate.discountAmount);

            selection.appliedPromotions.add(applied);
            selection.totalDiscount = scale(selection.totalDiscount.add(candidate.discountAmount));
            selection.codes.add(candidate.promotion.getCode());
            if (candidate.coupon != null) {
                selection.codes.add(candidate.coupon.getCode());
            }

            if (exclusionGroup != null && !exclusionGroup.isBlank()) {
                exclusionGroups.add(exclusionGroup);
            }
            if (!Boolean.TRUE.equals(candidate.promotion.getStackable())) {
                lockedByNonStackable = true;
            }
        }
        return selection;
    }

    private Coupon couponForPromotion(Promotion promotion, Map<String, Coupon> providedCoupons) {
        return providedCoupons.values().stream()
                .filter(coupon -> coupon.getPromotion().getId().equals(promotion.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean belongsToCategory(ProductVariant variant, Category category) {
        return variant != null
                && variant.getTemplate() != null
                && variant.getTemplate().getCategory() != null
                && Objects.equals(variant.getTemplate().getCategory().getId(), category.getId());
    }

    private Integer specificityScore(PricingRule rule) {
        int score = 0;
        if (rule.getCustomer() != null) score += 8;
        if (rule.getProductVariant() != null) score += 8;
        if (rule.getTerminal() != null) score += 4;
        if (rule.getWarehouse() != null) score += 3;
        if (rule.getCategory() != null) score += 3;
        if (rule.getCustomerCategory() != null) score += 2;
        if (rule.getSalesChannel() != null) score += 1;
        return score;
    }

    private BigDecimal percentageDiscount(BigDecimal base, BigDecimal percent, BigDecimal maxDiscount) {
        BigDecimal discount = base.multiply(scale(percent)).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return scale(discount.max(BigDecimal.ZERO));
    }

    private void mergeAppliedPromotions(List<AppliedPromotionEvaluation> target, Collection<AppliedPromotionEvaluation> source) {
        Map<String, AppliedPromotionEvaluation> byKey = target.stream().collect(Collectors.toMap(
                this::promotionMergeKey,
                applied -> applied,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        for (AppliedPromotionEvaluation applied : source) {
            String key = promotionMergeKey(applied);
            AppliedPromotionEvaluation existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, applied);
            } else {
                existing.setDiscountAmount(scale(existing.getDiscountAmount().add(applied.getDiscountAmount())));
            }
        }
        target.clear();
        target.addAll(byKey.values());
    }

    private String promotionMergeKey(AppliedPromotionEvaluation applied) {
        return applied.getPromotion().getId() + ":" + (applied.getCoupon() != null ? applied.getCoupon().getId() : "-");
    }

    private AppliedPromotionDto mapAppliedPromotion(AppliedPromotionEvaluation appliedPromotion) {
        AppliedPromotionDto dto = new AppliedPromotionDto();
        dto.setPromotionId(appliedPromotion.getPromotion().getId());
        dto.setPromotionName(appliedPromotion.getPromotion().getName());
        dto.setPromotionCode(appliedPromotion.getPromotion().getCode());
        dto.setDiscountType(appliedPromotion.getPromotion().getDiscountType());
        dto.setCouponId(appliedPromotion.getCoupon() != null ? appliedPromotion.getCoupon().getId() : null);
        dto.setCouponCode(appliedPromotion.getCoupon() != null ? appliedPromotion.getCoupon().getCode() : null);
        dto.setDiscountAmount(appliedPromotion.getDiscountAmount());
        dto.setStackable(appliedPromotion.getPromotion().getStackable());
        return dto;
    }

    private PricingPreviewLineDto mapLine(PricingEvaluationLine line) {
        PricingPreviewLineDto dto = new PricingPreviewLineDto();
        dto.setProductVariantId(line.getProductVariant().getId());
        dto.setSku(line.getProductVariant().getSku());
        dto.setQuantity(line.getQuantity());
        dto.setBaseUnitPrice(line.getBaseUnitPrice());
        dto.setFinalUnitPrice(line.getFinalUnitPrice());
        dto.setBaseLineAmount(line.getBaseLineAmount());
        dto.setLineDiscountAmount(line.getLineDiscountAmount());
        dto.setLineTotalAmount(line.getLineTotalAmount());
        dto.setAppliedPromotionCodes(new ArrayList<>(line.getAppliedPromotionCodes()));
        return dto;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(6, RoundingMode.HALF_UP);
    }

    private static class EvaluationItem {
        private final UUID productVariantId;
        private final BigDecimal quantity;
        private final BigDecimal requestedUnitPrice;
        private final BigDecimal manualLineDiscount;

        private EvaluationItem(UUID productVariantId, BigDecimal quantity, BigDecimal requestedUnitPrice, BigDecimal manualLineDiscount) {
            this.productVariantId = productVariantId;
            this.quantity = quantity;
            this.requestedUnitPrice = requestedUnitPrice;
            this.manualLineDiscount = manualLineDiscount;
        }
    }

    private static class PromotionCandidate {
        private Promotion promotion;
        private Coupon coupon;
        private BigDecimal discountAmount = BigDecimal.ZERO;
    }

    private static class LinePromotionSelection {
        private BigDecimal totalDiscount = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        private final List<String> codes = new ArrayList<>();
        private final List<AppliedPromotionEvaluation> appliedPromotions = new ArrayList<>();
    }

    private static class OrderPromotionSelection {
        private BigDecimal totalDiscount = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        private List<AppliedPromotionEvaluation> appliedPromotions = new ArrayList<>();
    }
}