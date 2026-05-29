package com.inventory.system.service.impl;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.DiscountAppliesToScope;
import com.inventory.system.common.entity.DiscountChannel;
import com.inventory.system.common.entity.DiscountCode;
import com.inventory.system.common.entity.DiscountCodeStatus;
import com.inventory.system.common.entity.DiscountCustomerEligibility;
import com.inventory.system.common.entity.DiscountCustomerInclusion;
import com.inventory.system.common.entity.DiscountCustomerInclusionScope;
import com.inventory.system.common.entity.DiscountInclusionMode;
import com.inventory.system.common.entity.DiscountKind;
import com.inventory.system.common.entity.DiscountMinPurchaseType;
import com.inventory.system.common.entity.DiscountProductInclusion;
import com.inventory.system.common.entity.DiscountProductInclusionScope;
import com.inventory.system.common.entity.DiscountRedemption;
import com.inventory.system.common.entity.DiscountRedemptionStatus;
import com.inventory.system.common.entity.DiscountStatus;
import com.inventory.system.common.entity.DiscountTier;
import com.inventory.system.common.entity.DiscountValueType;
import com.inventory.system.common.entity.PosSale;
import com.inventory.system.common.entity.PosTerminal;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.payload.AppliedDiscountDto;
import com.inventory.system.payload.CreatePosSaleRequest;
import com.inventory.system.payload.PosSaleItemRequest;
import com.inventory.system.payload.PricingPreviewItemRequest;
import com.inventory.system.payload.PricingPreviewLine;
import com.inventory.system.payload.PricingPreviewRequest;
import com.inventory.system.payload.PricingPreviewResponse;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.repository.DiscountCodeRepository;
import com.inventory.system.repository.DiscountRedemptionRepository;
import com.inventory.system.repository.DiscountRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.GiftCardService;
import com.inventory.system.service.PricingEngineService;
import com.inventory.system.service.PricingEvaluation;
import com.inventory.system.service.PricingEvaluationLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingEngineServiceImpl implements PricingEngineService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 6;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int ABUSE_REDEMPTION_THRESHOLD = 3;
    private static final long ABUSE_WINDOW_HOURS = 24;

    private final DiscountRepository discountRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountRedemptionRepository redemptionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final GiftCardService giftCardService;

    // ---------------- Public API ----------------

    @Override
    @Transactional(readOnly = true)
    public PricingEvaluation evaluateSalesOrder(Customer customer, Warehouse warehouse, SalesOrderRequest request) {
        PricingEvaluation eval = evaluate(
                customer,
                request.getCouponCodes(),
                null,
                null,
                DiscountChannel.ONLINE,
                LocalDateTime.now(),
                toLinesFromSalesOrder(request),
                BigDecimal.ZERO,
                null,
                false
        );
        attachGiftCardPreview(eval, request.getGiftCardCodes(), BigDecimal.ZERO);
        return eval;
    }

    @Override
    @Transactional(readOnly = true)
    public PricingEvaluation evaluatePosSale(Customer customer, Warehouse warehouse, PosTerminal terminal,
                                             CreatePosSaleRequest request, boolean hasManualDiscountOverride) {
        PricingEvaluation eval = evaluate(
                customer,
                request.getCouponCodes(),
                null,
                null,
                DiscountChannel.POS,
                LocalDateTime.now(),
                toLinesFromPos(request),
                BigDecimal.ZERO,
                null,
                hasManualDiscountOverride
        );
        attachGiftCardPreview(eval, request.getGiftCardCodes(), BigDecimal.ZERO);
        return eval;
    }

    private void attachGiftCardPreview(PricingEvaluation eval, List<String> giftCardCodes, BigDecimal shipping) {
        if (giftCardCodes == null || giftCardCodes.isEmpty()) return;
        BigDecimal due = eval.getNetSubtotal().add(nz(shipping)).subtract(eval.getShippingDiscount());
        if (due.signum() <= 0) return;
        java.util.LinkedHashMap<String, BigDecimal> preview = giftCardService.previewRedemption(giftCardCodes, due);
        BigDecimal sum = preview.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        eval.setGiftCardAmount(sum);
        eval.getAppliedGiftCardCodes().addAll(preview.keySet());
    }

    @Override
    @Transactional
    public void recordRedemptions(PricingEvaluation evaluation, SalesOrder salesOrder, PosSale posSale,
                                  Customer customer, SalesChannel channel, String referenceNumber) {
        for (PricingEvaluation.AppliedDiscount applied : evaluation.getAppliedDiscounts()) {
            if (applied.getAmount() == null || applied.getAmount().signum() <= 0) continue;

            DiscountRedemption r = new DiscountRedemption();
            r.setDiscount(applied.getDiscount());
            r.setDiscountCode(applied.getDiscountCode());
            r.setCustomer(customer);
            r.setSalesOrder(salesOrder);
            r.setPosSaleId(posSale != null ? posSale.getId() : null);
            r.setSalesChannel(channel);
            r.setStatus(DiscountRedemptionStatus.APPLIED);
            r.setDiscountAmount(applied.getAmount());
            r.setOrderSubtotal(evaluation.getBaseSubtotal());
            r.setRedeemedAt(LocalDateTime.now());

            if (customer != null && applied.getDiscountCode() != null) {
                long recent = redemptionRepository.countByDiscountCodeIdAndCustomerIdAndRedeemedAtAfter(
                        applied.getDiscountCode().getId(),
                        customer.getId(),
                        LocalDateTime.now().minusHours(ABUSE_WINDOW_HOURS)
                );
                if (recent >= ABUSE_REDEMPTION_THRESHOLD) {
                    r.setAbuseFlag(true);
                    r.setAbuseReason("Customer redeemed code " + (recent + 1) + " times in " + ABUSE_WINDOW_HOURS + "h");
                    r.setStatus(DiscountRedemptionStatus.FLAGGED);
                }
            }

            redemptionRepository.save(r);

            Discount d = applied.getDiscount();
            d.setUsedCount((d.getUsedCount() == null ? 0 : d.getUsedCount()) + 1);
            discountRepository.save(d);

            DiscountCode c = applied.getDiscountCode();
            if (c != null) {
                c.setRedeemedCount((c.getRedeemedCount() == null ? 0 : c.getRedeemedCount()) + 1);
                discountCodeRepository.save(c);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPreviewResponse preview(PricingPreviewRequest request) {
        List<EvalLine> evalLines = new ArrayList<>();
        if (request.items() != null) {
            Set<UUID> variantIds = new HashSet<>();
            for (PricingPreviewItemRequest it : request.items()) variantIds.add(it.productVariantId());
            Map<UUID, ProductVariant> variants = new HashMap<>();
            for (ProductVariant v : productVariantRepository.findAllById(variantIds)) variants.put(v.getId(), v);

            for (PricingPreviewItemRequest it : request.items()) {
                ProductVariant variant = variants.get(it.productVariantId());
                if (variant == null) continue;
                EvalLine line = new EvalLine();
                line.variant = variant;
                line.variantId = variant.getId();
                line.categoryId = variant.getTemplate() != null && variant.getTemplate().getCategory() != null
                        ? variant.getTemplate().getCategory().getId() : null;
                line.quantity = nz(it.quantity());
                line.baseUnitPrice = nz(it.unitPrice());
                evalLines.add(line);
            }
        }

        Customer customer = null;  // preview without explicit customer entity is fine
        PricingEvaluation eval = evaluate(
                customer,
                request.discountCodes(),
                request.customerId(),
                request.customerEmail(),
                request.channel() != null ? request.channel() : DiscountChannel.ONLINE,
                request.evaluationTime() != null ? request.evaluationTime() : LocalDateTime.now(),
                evalLines,
                nz(request.shippingAmount()),
                request.shippingCountry(),
                false
        );

        List<PricingPreviewLine> outLines = new ArrayList<>();
        for (PricingEvaluationLine l : eval.getLines()) {
            outLines.add(new PricingPreviewLine(
                    l.getProductVariantId(),
                    l.getQuantity(),
                    l.getBaseUnitPrice(),
                    l.getBaseUnitPrice().multiply(l.getQuantity()).setScale(SCALE, RM),
                    l.getLineDiscountAmount(),
                    l.getLineTotalAmount(),
                    new ArrayList<>(l.getAppliedPromotionCodes())
            ));
        }

        List<AppliedDiscountDto> applied = new ArrayList<>();
        for (PricingEvaluation.AppliedDiscount a : eval.getAppliedDiscounts()) {
            applied.add(new AppliedDiscountDto(
                    a.getDiscount().getId(),
                    a.getDiscountCode() != null ? a.getDiscountCode().getId() : null,
                    a.getDiscountCode() != null ? a.getDiscountCode().getCode() : null,
                    a.getDiscount().getName(),
                    a.getDiscount().getKind(),
                    a.getAmount(),
                    a.getMessage()
            ));
        }

        BigDecimal shipping = nz(request.shippingAmount());
        BigDecimal beforeGift = eval.getNetSubtotal().add(shipping).subtract(eval.getShippingDiscount()).setScale(SCALE, RM);
        if (beforeGift.signum() < 0) beforeGift = BigDecimal.ZERO;

        java.util.LinkedHashMap<String, BigDecimal> giftPreview = giftCardService.previewRedemption(request.giftCardCodes(), beforeGift);
        BigDecimal giftAmount = giftPreview.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        eval.setGiftCardAmount(giftAmount);
        eval.getAppliedGiftCardCodes().addAll(giftPreview.keySet());

        BigDecimal grand = beforeGift.subtract(giftAmount).setScale(SCALE, RM);
        if (grand.signum() < 0) grand = BigDecimal.ZERO;

        return new PricingPreviewResponse(
                eval.getBaseSubtotal(),
                eval.getTotalDiscount(),
                shipping,
                eval.getShippingDiscount(),
                giftAmount,
                grand,
                outLines,
                applied,
                rejectedCodes(request.discountCodes(), eval),
                eval.getWarnings()
        );
    }

    // ---------------- Core evaluation ----------------

    private PricingEvaluation evaluate(Customer customer,
                                       List<String> rawCodes,
                                       UUID previewCustomerId,
                                       String customerEmail,
                                       DiscountChannel channel,
                                       LocalDateTime now,
                                       List<EvalLine> lines,
                                       BigDecimal shippingAmount,
                                       String shippingCountry,
                                       boolean manualOverride) {
        PricingEvaluation eval = new PricingEvaluation();
        if (lines == null || lines.isEmpty()) {
            buildEmptyEvaluation(eval);
            return eval;
        }

        // Initial per-line base totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            l.lineDiscount = BigDecimal.ZERO;
            subtotal = subtotal.add(l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM));
        }
        eval.setBaseSubtotal(subtotal.setScale(SCALE, RM));

        // Collect candidate discounts: codes user supplied + auto-apply discounts
        Set<String> normalizedCodes = normalizeCodes(rawCodes);
        List<Candidate> candidates = new ArrayList<>();

        for (String code : normalizedCodes) {
            Optional<DiscountCode> opt = discountCodeRepository.findByCodeIgnoreCase(code);
            if (opt.isPresent()) {
                DiscountCode dc = opt.get();
                Discount d = dc.getDiscount();
                if (d != null) candidates.add(new Candidate(d, dc, code));
            } else {
                eval.getWarnings().add("Unknown code: " + code);
            }
        }
        for (Discount d : discountRepository.findByAutoApplyTrueAndStatus(DiscountStatus.ACTIVE)) {
            candidates.add(new Candidate(d, null, null));
        }

        // Filter to applicable based on status, time, schedule, channel, customer, min purchase, usage limits
        UUID customerId = customer != null ? customer.getId() : previewCustomerId;
        List<Candidate> eligible = new ArrayList<>();
        for (Candidate c : candidates) {
            String reject = checkEligibility(c, customerId, customer, channel, now, subtotal, lines);
            if (reject == null) eligible.add(c);
            else if (c.code != null) eval.getWarnings().add(c.code + ": " + reject);
        }

        // Apply: sort by priority desc, then by potential discount magnitude desc
        eligible.sort(Comparator
                .comparingInt((Candidate c) -> c.discount.getPriority() == null ? 100 : c.discount.getPriority())
                .reversed());

        // Track stacking/exclusion
        Set<String> usedExclusionGroups = new HashSet<>();
        boolean orderDiscountApplied = false;
        boolean productDiscountApplied = false;
        boolean shippingDiscountApplied = false;

        BigDecimal totalOrderDiscount = BigDecimal.ZERO;
        BigDecimal totalShippingDiscount = BigDecimal.ZERO;

        for (Candidate c : eligible) {
            Discount d = c.discount;

            // Exclusion group enforcement
            if (d.getExclusionGroup() != null && !d.getExclusionGroup().isBlank()
                    && usedExclusionGroups.contains(d.getExclusionGroup())) {
                if (c.code != null) eval.getWarnings().add(c.code + ": excluded by another applied discount in same group");
                continue;
            }

            DiscountKind kind = d.getKind();
            boolean isOrder = isOrderKind(kind);
            boolean isProduct = isProductKind(kind);
            boolean isShipping = kind == DiscountKind.FREE_SHIPPING;

            // Combine rules: if already applied something of same family, require combine flag
            if (isOrder && orderDiscountApplied && !Boolean.TRUE.equals(d.getCombineWithOrderDiscounts())) continue;
            if (isProduct && productDiscountApplied && !Boolean.TRUE.equals(d.getCombineWithProductDiscounts())) continue;
            if (isShipping && shippingDiscountApplied && !Boolean.TRUE.equals(d.getCombineWithShippingDiscounts())) continue;
            // Cross-family stackable check
            if (!Boolean.TRUE.equals(d.getStackable())) {
                if (isOrder && (productDiscountApplied || shippingDiscountApplied)) continue;
                if (isProduct && (orderDiscountApplied || shippingDiscountApplied)) continue;
                if (isShipping && (orderDiscountApplied || productDiscountApplied)) continue;
            }

            BigDecimal applied = applyDiscount(d, lines, subtotal.subtract(totalOrderDiscount), shippingAmount, shippingCountry);
            if (applied == null || applied.signum() <= 0) {
                if (c.code != null) eval.getWarnings().add(c.code + ": no discount applicable");
                continue;
            }

            // Cap shipping discount at shipping amount; cap order/product at remaining subtotal
            if (isShipping) {
                BigDecimal remainingShip = shippingAmount.subtract(totalShippingDiscount);
                if (applied.compareTo(remainingShip) > 0) applied = remainingShip;
                if (applied.signum() > 0) {
                    totalShippingDiscount = totalShippingDiscount.add(applied);
                    shippingDiscountApplied = true;
                }
            } else if (isOrder) {
                BigDecimal remainingSubtotal = subtotal.subtract(totalOrderDiscount).subtract(sumLineDiscounts(lines));
                if (applied.compareTo(remainingSubtotal) > 0) applied = remainingSubtotal;
                if (applied.signum() > 0) {
                    totalOrderDiscount = totalOrderDiscount.add(applied);
                    orderDiscountApplied = true;
                }
            } else if (isProduct) {
                // Line discounts already mutated by applyDiscount
                productDiscountApplied = true;
            }

            if (applied.signum() > 0) {
                PricingEvaluation.AppliedDiscount ap = new PricingEvaluation.AppliedDiscount(d, c.discountCode, applied);
                ap.setMessage(c.code != null ? "Code " + c.code + " applied" : "Auto-applied: " + d.getName());
                eval.getAppliedDiscounts().add(ap);
                if (c.code != null) eval.getAppliedCouponCodes().add(c.code);
                if (d.getExclusionGroup() != null && !d.getExclusionGroup().isBlank()) {
                    usedExclusionGroups.add(d.getExclusionGroup());
                }
            }
        }

        // Build per-line PricingEvaluationLine output
        BigDecimal totalLineDiscount = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            BigDecimal lineGross = l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM);
            BigDecimal lineNet = lineGross.subtract(nz(l.lineDiscount));
            if (lineNet.signum() < 0) lineNet = BigDecimal.ZERO;
            BigDecimal finalUnit = l.quantity.signum() == 0
                    ? BigDecimal.ZERO
                    : lineNet.divide(l.quantity, SCALE, RM);

            PricingEvaluationLine out = PricingEvaluationLine.builder()
                    .productVariant(l.variant)
                    .productVariantId(l.variantId)
                    .quantity(l.quantity)
                    .baseUnitPrice(l.baseUnitPrice)
                    .finalUnitPrice(finalUnit)
                    .lineDiscountAmount(nz(l.lineDiscount))
                    .lineTotalAmount(lineNet)
                    .appliedPromotionCodes(new ArrayList<>(l.appliedCodes))
                    .build();
            eval.getLines().add(out);
            totalLineDiscount = totalLineDiscount.add(nz(l.lineDiscount));
        }

        BigDecimal totalDiscount = totalLineDiscount.add(totalOrderDiscount).setScale(SCALE, RM);
        BigDecimal net = subtotal.subtract(totalDiscount);
        if (net.signum() < 0) net = BigDecimal.ZERO;
        eval.setTotalDiscount(totalDiscount);
        eval.setNetSubtotal(net.setScale(SCALE, RM));
        eval.setShippingDiscount(totalShippingDiscount.setScale(SCALE, RM));
        return eval;
    }

    // ---------------- Eligibility ----------------

    private String checkEligibility(Candidate c, UUID customerId, Customer customer,
                                    DiscountChannel channel, LocalDateTime now,
                                    BigDecimal subtotal, List<EvalLine> lines) {
        Discount d = c.discount;
        if (d.getStatus() != DiscountStatus.ACTIVE && d.getStatus() != DiscountStatus.SCHEDULED) return "not active";
        if (d.getStartsAt() != null && now.isBefore(d.getStartsAt())) return "not yet started";
        if (d.getEndsAt() != null && now.isAfter(d.getEndsAt())) return "expired";

        DiscountChannel salesChannel = d.getSalesChannel();
        if (salesChannel != null && salesChannel != DiscountChannel.ALL && salesChannel != channel) return "wrong channel";

        if (!matchesSchedule(d, now)) return "outside schedule window";

        // Code state
        if (c.discountCode != null) {
            DiscountCode dc = c.discountCode;
            if (dc.getStatus() != DiscountCodeStatus.ACTIVE) return "code not active";
            if (dc.getValidFrom() != null && now.isBefore(dc.getValidFrom())) return "code not yet valid";
            if (dc.getValidTo() != null && now.isAfter(dc.getValidTo())) return "code expired";
            if (dc.getMaxRedemptions() != null && dc.getRedeemedCount() != null
                    && dc.getRedeemedCount() >= dc.getMaxRedemptions()) return "code fully redeemed";
            if (customerId != null && dc.getMaxRedemptionsPerCustomer() != null) {
                long used = redemptionRepository.countByDiscountCodeIdAndCustomerIdAndStatus(
                        dc.getId(), customerId, DiscountRedemptionStatus.APPLIED);
                if (used >= dc.getMaxRedemptionsPerCustomer()) return "per-customer code limit reached";
            }
        } else if (Boolean.FALSE.equals(d.getAutoApply())) {
            return "requires a code";
        }

        // Discount-level usage limits
        if (d.getUsageLimitTotal() != null && d.getUsedCount() != null
                && d.getUsedCount() >= d.getUsageLimitTotal()) return "discount fully redeemed";
        if (customerId != null && d.getUsageLimitPerCustomer() != null) {
            long used = redemptionRepository.countByDiscountIdAndCustomerIdAndStatus(
                    d.getId(), customerId, DiscountRedemptionStatus.APPLIED);
            if (used >= d.getUsageLimitPerCustomer()) return "per-customer limit reached";
        }

        // Customer eligibility
        if (!checkCustomerEligibility(d, customerId, customer)) return "customer not eligible";

        // Minimum purchase
        if (d.getMinPurchaseType() == DiscountMinPurchaseType.AMOUNT && d.getMinPurchaseAmount() != null
                && subtotal.compareTo(d.getMinPurchaseAmount()) < 0) return "minimum purchase amount not met";
        if (d.getMinPurchaseType() == DiscountMinPurchaseType.QUANTITY && d.getMinPurchaseQuantity() != null) {
            BigDecimal totalQty = BigDecimal.ZERO;
            for (EvalLine l : lines) totalQty = totalQty.add(l.quantity);
            if (totalQty.compareTo(d.getMinPurchaseQuantity()) < 0) return "minimum quantity not met";
        }

        // Applies-to scoping (PRODUCTS/CATEGORIES): at least one line must match include list (and not be excluded)
        if (d.getAppliesToScope() == DiscountAppliesToScope.PRODUCTS
                || d.getAppliesToScope() == DiscountAppliesToScope.CATEGORIES) {
            boolean anyMatch = false;
            for (EvalLine l : lines) if (lineMatchesInclusions(d, l)) { anyMatch = true; break; }
            if (!anyMatch) return "no eligible products in cart";
        }

        return null;
    }

    private boolean checkCustomerEligibility(Discount d, UUID customerId, Customer customer) {
        DiscountCustomerEligibility elig = d.getCustomerEligibility();
        if (elig == null || elig == DiscountCustomerEligibility.ALL) return checkCustomerInclusionLists(d, customerId, customer);

        if (elig == DiscountCustomerEligibility.FIRST_ORDER_ONLY) {
            if (customerId == null) return false;
            long previous = salesOrderRepository.countByCustomerId(customerId);
            if (previous > 0) return false;
            return checkCustomerInclusionLists(d, customerId, customer);
        }

        if (elig == DiscountCustomerEligibility.SPECIFIC_CUSTOMERS) {
            if (customerId == null) return false;
            boolean inAny = false;
            for (DiscountCustomerInclusion inc : d.getCustomerInclusions()) {
                if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER
                        && inc.getMode() == DiscountInclusionMode.INCLUDE
                        && customerId.toString().equalsIgnoreCase(inc.getEntityId())) inAny = true;
                if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER
                        && inc.getMode() == DiscountInclusionMode.EXCLUDE
                        && customerId.toString().equalsIgnoreCase(inc.getEntityId())) return false;
            }
            return inAny;
        }

        if (elig == DiscountCustomerEligibility.CUSTOMER_GROUPS) {
            if (customer == null || customer.getCategory() == null) return false;
            String cat = customer.getCategory().name();
            boolean inAny = false;
            for (DiscountCustomerInclusion inc : d.getCustomerInclusions()) {
                if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER_CATEGORY
                        && inc.getMode() == DiscountInclusionMode.INCLUDE
                        && cat.equalsIgnoreCase(inc.getEntityId())) inAny = true;
                if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER_CATEGORY
                        && inc.getMode() == DiscountInclusionMode.EXCLUDE
                        && cat.equalsIgnoreCase(inc.getEntityId())) return false;
            }
            return inAny;
        }

        return true;
    }

    private boolean checkCustomerInclusionLists(Discount d, UUID customerId, Customer customer) {
        // Even under ALL eligibility, EXCLUDE entries still bar specific customers/groups
        if (d.getCustomerInclusions() == null || d.getCustomerInclusions().isEmpty()) return true;
        for (DiscountCustomerInclusion inc : d.getCustomerInclusions()) {
            if (inc.getMode() != DiscountInclusionMode.EXCLUDE) continue;
            if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER && customerId != null
                    && customerId.toString().equalsIgnoreCase(inc.getEntityId())) return false;
            if (inc.getScope() == DiscountCustomerInclusionScope.CUSTOMER_CATEGORY && customer != null
                    && customer.getCategory() != null
                    && customer.getCategory().name().equalsIgnoreCase(inc.getEntityId())) return false;
        }
        return true;
    }

    private boolean matchesSchedule(Discount d, LocalDateTime now) {
        ZoneId zone = ZoneId.systemDefault();
        if (d.getScheduleTimezone() != null && !d.getScheduleTimezone().isBlank()) {
            try { zone = ZoneId.of(d.getScheduleTimezone()); } catch (Exception ignored) { /* fall back */ }
        }
        ZonedDateTime localNow = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone);

        if (d.getScheduleDaysOfWeek() != null && !d.getScheduleDaysOfWeek().isBlank()) {
            Set<DayOfWeek> allowed = new HashSet<>();
            for (String tok : d.getScheduleDaysOfWeek().split(",")) {
                try { allowed.add(DayOfWeek.valueOf(tok.trim().toUpperCase(Locale.ROOT))); } catch (Exception ignored) {}
            }
            if (!allowed.isEmpty() && !allowed.contains(localNow.getDayOfWeek())) return false;
        }

        LocalTime startT = d.getScheduleStartTime();
        LocalTime endT = d.getScheduleEndTime();
        if (startT != null || endT != null) {
            LocalTime nowT = localNow.toLocalTime();
            if (startT != null && endT != null) {
                if (endT.isAfter(startT)) {
                    if (nowT.isBefore(startT) || nowT.isAfter(endT)) return false;
                } else {
                    // Window wraps midnight
                    if (nowT.isBefore(startT) && nowT.isAfter(endT)) return false;
                }
            } else if (startT != null) {
                if (nowT.isBefore(startT)) return false;
            } else {
                if (nowT.isAfter(endT)) return false;
            }
        }
        return true;
    }

    private boolean lineMatchesInclusions(Discount d, EvalLine line) {
        boolean hasInclude = false;
        boolean anyInclude = false;
        for (DiscountProductInclusion inc : d.getProductInclusions()) {
            if (inc.getMode() == DiscountInclusionMode.EXCLUDE) {
                if (matchesEntity(inc, line)) return false;
                continue;
            }
            hasInclude = true;
            if (matchesEntity(inc, line)) anyInclude = true;
        }
        if (!hasInclude) return d.getAppliesToScope() == DiscountAppliesToScope.ALL;
        return anyInclude;
    }

    private boolean matchesEntity(DiscountProductInclusion inc, EvalLine line) {
        if (inc.getScope() == DiscountProductInclusionScope.VARIANT) return inc.getEntityId().equals(line.variantId);
        if (inc.getScope() == DiscountProductInclusionScope.PRODUCT) {
            return line.variant != null && line.variant.getTemplate() != null
                    && inc.getEntityId().equals(line.variant.getTemplate().getId());
        }
        if (inc.getScope() == DiscountProductInclusionScope.CATEGORY) {
            return line.categoryId != null && inc.getEntityId().equals(line.categoryId);
        }
        return false;
    }

    // ---------------- Discount application ----------------

    private BigDecimal applyDiscount(Discount d, List<EvalLine> lines, BigDecimal remainingSubtotal,
                                     BigDecimal shippingAmount, String shippingCountry) {
        switch (d.getKind()) {
            case FREE_SHIPPING:
                return applyFreeShipping(d, shippingAmount, shippingCountry);
            case AMOUNT_OFF_ORDER:
                return applyAmountOffOrder(d, remainingSubtotal);
            case AMOUNT_OFF_PRODUCTS:
                return applyAmountOffProducts(d, lines);
            case BOGO:
                return applyBogo(d, lines);
            case BUNDLE:
                return applyBundle(d, lines);
            case TIERED_AMOUNT_OFF_ORDER:
                return applyTieredOrder(d, remainingSubtotal);
            case TIERED_AMOUNT_OFF_PRODUCTS:
                return applyTieredProducts(d, lines);
            case REFERRAL_REFERRER:
            case REFERRAL_REFEREE:
                // Referral discounts are structurally AMOUNT_OFF_ORDER; engine applies their value field.
                return applyAmountOffOrder(d, remainingSubtotal);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal applyFreeShipping(Discount d, BigDecimal shippingAmount, String shippingCountry) {
        if (shippingAmount == null || shippingAmount.signum() <= 0) return BigDecimal.ZERO;
        if (d.getFreeShippingCountries() != null && !d.getFreeShippingCountries().isBlank()) {
            if (shippingCountry == null) return BigDecimal.ZERO;
            Set<String> allowed = new HashSet<>();
            for (String c : d.getFreeShippingCountries().split(",")) allowed.add(c.trim().toUpperCase(Locale.ROOT));
            if (!allowed.contains(shippingCountry.toUpperCase(Locale.ROOT))) return BigDecimal.ZERO;
        }
        BigDecimal cap = d.getFreeShippingMaxAmount();
        BigDecimal amount = (cap != null && cap.signum() > 0) ? cap.min(shippingAmount) : shippingAmount;
        return amount.setScale(SCALE, RM);
    }

    private BigDecimal applyAmountOffOrder(Discount d, BigDecimal remainingSubtotal) {
        if (remainingSubtotal == null || remainingSubtotal.signum() <= 0) return BigDecimal.ZERO;
        if (d.getValueType() == null || d.getValue() == null) return BigDecimal.ZERO;
        BigDecimal amt = computeValue(d.getValueType(), d.getValue(), remainingSubtotal);
        return cap(amt, d.getMaxDiscountAmount()).setScale(SCALE, RM);
    }

    private BigDecimal applyAmountOffProducts(Discount d, List<EvalLine> lines) {
        if (d.getValueType() == null || d.getValue() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            if (!lineMatchesInclusions(d, l)) continue;
            BigDecimal lineGross = l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM);
            BigDecimal remaining = lineGross.subtract(nz(l.lineDiscount));
            if (remaining.signum() <= 0) continue;
            BigDecimal amt = computeValue(d.getValueType(), d.getValue(), remaining);
            amt = cap(amt, d.getMaxDiscountAmount());
            if (amt.compareTo(remaining) > 0) amt = remaining;
            l.lineDiscount = nz(l.lineDiscount).add(amt);
            l.addAppliedCode(d);
            total = total.add(amt);
        }
        return total.setScale(SCALE, RM);
    }

    private BigDecimal applyBogo(Discount d, List<EvalLine> lines) {
        if (d.getBogoBuyQuantity() == null || d.getBogoGetQuantity() == null) return BigDecimal.ZERO;
        BigDecimal buyQty = d.getBogoBuyQuantity();
        BigDecimal getQty = d.getBogoGetQuantity();
        DiscountValueType getType = d.getBogoGetValueType() != null ? d.getBogoGetValueType() : DiscountValueType.PERCENTAGE;
        BigDecimal getValue = d.getBogoGetValue() != null ? d.getBogoGetValue() : HUNDRED;

        BigDecimal total = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            if (!lineMatchesInclusions(d, l)) continue;
            BigDecimal groups = l.quantity.divide(buyQty.add(getQty), 0, RoundingMode.FLOOR);
            if (groups.signum() <= 0) continue;
            BigDecimal discountedUnits = groups.multiply(getQty);
            BigDecimal perUnitDiscount = computeValue(getType, getValue, l.baseUnitPrice);
            if (perUnitDiscount.compareTo(l.baseUnitPrice) > 0) perUnitDiscount = l.baseUnitPrice;
            BigDecimal amt = perUnitDiscount.multiply(discountedUnits).setScale(SCALE, RM);
            BigDecimal lineGross = l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM);
            BigDecimal remaining = lineGross.subtract(nz(l.lineDiscount));
            if (amt.compareTo(remaining) > 0) amt = remaining;
            l.lineDiscount = nz(l.lineDiscount).add(amt);
            l.addAppliedCode(d);
            total = total.add(amt);
        }
        return total.setScale(SCALE, RM);
    }

    private BigDecimal applyBundle(Discount d, List<EvalLine> lines) {
        if (d.getBundleQuantity() == null || d.getBundlePrice() == null) return BigDecimal.ZERO;
        BigDecimal bundleQty = d.getBundleQuantity();
        BigDecimal bundlePrice = d.getBundlePrice();
        BigDecimal total = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            if (!lineMatchesInclusions(d, l)) continue;
            BigDecimal bundles = l.quantity.divide(bundleQty, 0, RoundingMode.FLOOR);
            if (bundles.signum() <= 0) continue;
            BigDecimal normalCost = l.baseUnitPrice.multiply(bundleQty).multiply(bundles);
            BigDecimal bundleCost = bundlePrice.multiply(bundles);
            BigDecimal amt = normalCost.subtract(bundleCost);
            if (amt.signum() <= 0) continue;
            BigDecimal lineGross = l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM);
            BigDecimal remaining = lineGross.subtract(nz(l.lineDiscount));
            if (amt.compareTo(remaining) > 0) amt = remaining;
            l.lineDiscount = nz(l.lineDiscount).add(amt);
            l.addAppliedCode(d);
            total = total.add(amt);
        }
        return total.setScale(SCALE, RM);
    }

    private BigDecimal applyTieredOrder(Discount d, BigDecimal remainingSubtotal) {
        DiscountTier tier = pickTierForAmount(d, remainingSubtotal);
        if (tier == null) return BigDecimal.ZERO;
        BigDecimal amt = computeValue(tier.getValueType(), tier.getValue(), remainingSubtotal);
        return cap(amt, d.getMaxDiscountAmount()).setScale(SCALE, RM);
    }

    private BigDecimal applyTieredProducts(Discount d, List<EvalLine> lines) {
        BigDecimal subtotalEligible = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            if (!lineMatchesInclusions(d, l)) continue;
            BigDecimal gross = l.baseUnitPrice.multiply(l.quantity);
            subtotalEligible = subtotalEligible.add(gross.subtract(nz(l.lineDiscount)));
        }
        DiscountTier tier = pickTierForAmount(d, subtotalEligible);
        if (tier == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (EvalLine l : lines) {
            if (!lineMatchesInclusions(d, l)) continue;
            BigDecimal lineGross = l.baseUnitPrice.multiply(l.quantity).setScale(SCALE, RM);
            BigDecimal remaining = lineGross.subtract(nz(l.lineDiscount));
            if (remaining.signum() <= 0) continue;
            BigDecimal amt = computeValue(tier.getValueType(), tier.getValue(), remaining);
            amt = cap(amt, d.getMaxDiscountAmount());
            if (amt.compareTo(remaining) > 0) amt = remaining;
            l.lineDiscount = nz(l.lineDiscount).add(amt);
            l.addAppliedCode(d);
            total = total.add(amt);
        }
        return total.setScale(SCALE, RM);
    }

    private DiscountTier pickTierForAmount(Discount d, BigDecimal amount) {
        if (d.getTiers() == null || d.getTiers().isEmpty()) return null;
        List<DiscountTier> sorted = new ArrayList<>(d.getTiers());
        sorted.sort(Comparator.comparing((DiscountTier t) -> nz(t.getMinSubtotal())).reversed());
        for (DiscountTier t : sorted) {
            if (t.getMinSubtotal() == null || amount.compareTo(t.getMinSubtotal()) >= 0) return t;
        }
        return null;
    }

    private BigDecimal computeValue(DiscountValueType type, BigDecimal value, BigDecimal base) {
        if (type == DiscountValueType.PERCENTAGE) {
            return base.multiply(value).divide(HUNDRED, SCALE, RM);
        }
        return value.setScale(SCALE, RM);
    }

    private BigDecimal cap(BigDecimal amt, BigDecimal cap) {
        if (cap == null || cap.signum() <= 0) return amt;
        return amt.compareTo(cap) > 0 ? cap : amt;
    }

    // ---------------- Helpers ----------------

    private List<EvalLine> toLinesFromSalesOrder(SalesOrderRequest req) {
        List<EvalLine> out = new ArrayList<>();
        if (req.getItems() == null) return out;
        Set<UUID> ids = new HashSet<>();
        for (SalesOrderItemRequest it : req.getItems()) ids.add(it.getProductVariantId());
        Map<UUID, ProductVariant> variants = new HashMap<>();
        for (ProductVariant v : productVariantRepository.findAllById(ids)) variants.put(v.getId(), v);
        for (SalesOrderItemRequest it : req.getItems()) {
            ProductVariant v = variants.get(it.getProductVariantId());
            if (v == null) continue;
            EvalLine l = new EvalLine();
            l.variant = v;
            l.variantId = v.getId();
            l.categoryId = v.getTemplate() != null && v.getTemplate().getCategory() != null
                    ? v.getTemplate().getCategory().getId() : null;
            l.quantity = nz(it.getQuantity());
            l.baseUnitPrice = nz(it.getUnitPrice());
            out.add(l);
        }
        return out;
    }

    private List<EvalLine> toLinesFromPos(CreatePosSaleRequest req) {
        List<EvalLine> out = new ArrayList<>();
        if (req.getItems() == null) return out;
        Set<UUID> ids = new HashSet<>();
        for (PosSaleItemRequest it : req.getItems()) ids.add(it.getProductVariantId());
        Map<UUID, ProductVariant> variants = new HashMap<>();
        for (ProductVariant v : productVariantRepository.findAllById(ids)) variants.put(v.getId(), v);
        for (PosSaleItemRequest it : req.getItems()) {
            ProductVariant v = variants.get(it.getProductVariantId());
            if (v == null) continue;
            EvalLine l = new EvalLine();
            l.variant = v;
            l.variantId = v.getId();
            l.categoryId = v.getTemplate() != null && v.getTemplate().getCategory() != null
                    ? v.getTemplate().getCategory().getId() : null;
            l.quantity = nz(it.getQuantity());
            l.baseUnitPrice = nz(it.getUnitPrice());
            // Honor any per-line manual discount the cashier set
            l.lineDiscount = nz(it.getLineDiscount());
            out.add(l);
        }
        return out;
    }

    private Set<String> normalizeCodes(List<String> rawCodes) {
        Set<String> out = new LinkedHashSet<>();
        if (rawCodes == null) return out;
        for (String r : rawCodes) {
            if (r == null) continue;
            String t = r.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void buildEmptyEvaluation(PricingEvaluation eval) {
        eval.setBaseSubtotal(BigDecimal.ZERO);
        eval.setTotalDiscount(BigDecimal.ZERO);
        eval.setNetSubtotal(BigDecimal.ZERO);
        eval.setShippingDiscount(BigDecimal.ZERO);
    }

    private BigDecimal sumLineDiscounts(List<EvalLine> lines) {
        BigDecimal s = BigDecimal.ZERO;
        for (EvalLine l : lines) s = s.add(nz(l.lineDiscount));
        return s;
    }

    private List<String> rejectedCodes(List<String> input, PricingEvaluation eval) {
        if (input == null) return List.of();
        List<String> rejected = new ArrayList<>();
        for (String c : input) if (!eval.getAppliedCouponCodes().contains(c.trim())) rejected.add(c.trim());
        return rejected;
    }

    private boolean isOrderKind(DiscountKind k) {
        return k == DiscountKind.AMOUNT_OFF_ORDER || k == DiscountKind.TIERED_AMOUNT_OFF_ORDER
                || k == DiscountKind.REFERRAL_REFERRER || k == DiscountKind.REFERRAL_REFEREE;
    }

    private boolean isProductKind(DiscountKind k) {
        return k == DiscountKind.AMOUNT_OFF_PRODUCTS || k == DiscountKind.TIERED_AMOUNT_OFF_PRODUCTS
                || k == DiscountKind.BOGO || k == DiscountKind.BUNDLE;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static class EvalLine {
        ProductVariant variant;
        UUID variantId;
        UUID categoryId;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal baseUnitPrice = BigDecimal.ZERO;
        BigDecimal lineDiscount = BigDecimal.ZERO;
        LinkedHashSet<String> appliedCodes = new LinkedHashSet<>();

        void addAppliedCode(Discount d) {
            String label = d.getName();
            if (label != null && !label.isBlank()) appliedCodes.add(label);
        }
    }

    private static class Candidate {
        final Discount discount;
        final DiscountCode discountCode;
        final String code;

        Candidate(Discount discount, DiscountCode discountCode, String code) {
            this.discount = discount;
            this.discountCode = discountCode;
            this.code = code;
        }
    }
}
