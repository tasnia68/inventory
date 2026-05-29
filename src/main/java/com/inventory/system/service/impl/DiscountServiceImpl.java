package com.inventory.system.service.impl;

import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.DiscountChannel;
import com.inventory.system.common.entity.DiscountCode;
import com.inventory.system.common.entity.DiscountCodeStatus;
import com.inventory.system.common.entity.DiscountCustomerInclusion;
import com.inventory.system.common.entity.DiscountInclusionMode;
import com.inventory.system.common.entity.DiscountProductInclusion;
import com.inventory.system.common.entity.DiscountRedemption;
import com.inventory.system.common.entity.DiscountRedemptionStatus;
import com.inventory.system.common.entity.DiscountStatus;
import com.inventory.system.common.entity.DiscountTier;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.CreateDiscountCodeRequest;
import com.inventory.system.payload.CreateDiscountRequest;
import com.inventory.system.payload.DiscountAnalyticsDto;
import com.inventory.system.payload.DiscountCodeDto;
import com.inventory.system.payload.DiscountCustomerInclusionDto;
import com.inventory.system.payload.DiscountDto;
import com.inventory.system.payload.DiscountProductInclusionDto;
import com.inventory.system.payload.DiscountTierDto;
import com.inventory.system.repository.DiscountCodeRepository;
import com.inventory.system.repository.DiscountRedemptionRepository;
import com.inventory.system.repository.DiscountRepository;
import com.inventory.system.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountRedemptionRepository redemptionRepository;

    @Override
    @Transactional
    public DiscountDto create(CreateDiscountRequest req) {
        Discount d = new Discount();
        applyRequest(d, req);
        d = discountRepository.save(d);
        return toDto(d);
    }

    @Override
    @Transactional
    public DiscountDto update(UUID id, CreateDiscountRequest req) {
        Discount d = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        applyRequest(d, req);
        d = discountRepository.save(d);
        return toDto(d);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Discount d = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        long redemptions = redemptionRepository.countByDiscountIdAndStatus(id, DiscountRedemptionStatus.APPLIED);
        if (redemptions > 0) {
            throw new BadRequestException("Cannot delete discount with " + redemptions + " applied redemptions; pause instead");
        }
        discountRepository.delete(d);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountDto get(UUID id) {
        return discountRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountDto> list() {
        return discountRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountDto> listAvailable(DiscountChannel channel) {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findByStatus(DiscountStatus.ACTIVE).stream()
                .filter(d -> d.getStartsAt() == null || !now.isBefore(d.getStartsAt()))
                .filter(d -> d.getEndsAt() == null || !now.isAfter(d.getEndsAt()))
                .filter(d -> channel == null || d.getSalesChannel() == null
                        || d.getSalesChannel() == DiscountChannel.ALL
                        || d.getSalesChannel() == channel)
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DiscountCodeDto createCode(UUID discountId, CreateDiscountCodeRequest req) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        if (req.code() == null || req.code().isBlank()) throw new BadRequestException("Code required");
        if (discountCodeRepository.findByCodeIgnoreCase(req.code().trim()).isPresent()) {
            throw new BadRequestException("Code already exists");
        }
        DiscountCode c = new DiscountCode();
        c.setDiscount(discount);
        c.setCode(req.code().trim().toUpperCase());
        c.setStatus(req.status() != null ? req.status() : DiscountCodeStatus.ACTIVE);
        c.setValidFrom(req.validFrom());
        c.setValidTo(req.validTo());
        c.setMaxRedemptions(req.maxRedemptions());
        c.setMaxRedemptionsPerCustomer(req.maxRedemptionsPerCustomer());
        c.setNotes(req.notes());
        c = discountCodeRepository.save(c);
        return toCodeDto(c);
    }

    @Override
    @Transactional
    public DiscountCodeDto updateCode(UUID codeId, CreateDiscountCodeRequest req) {
        DiscountCode c = discountCodeRepository.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found"));
        if (req.code() != null && !req.code().isBlank()) c.setCode(req.code().trim().toUpperCase());
        if (req.status() != null) c.setStatus(req.status());
        c.setValidFrom(req.validFrom());
        c.setValidTo(req.validTo());
        c.setMaxRedemptions(req.maxRedemptions());
        c.setMaxRedemptionsPerCustomer(req.maxRedemptionsPerCustomer());
        c.setNotes(req.notes());
        return toCodeDto(discountCodeRepository.save(c));
    }

    @Override
    @Transactional
    public void deleteCode(UUID codeId) {
        DiscountCode c = discountCodeRepository.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found"));
        discountCodeRepository.delete(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountCodeDto> listCodes(UUID discountId) {
        if (discountId != null) {
            return discountCodeRepository.findByDiscountId(discountId).stream().map(this::toCodeDto).toList();
        }
        return discountCodeRepository.findAll().stream().map(this::toCodeDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountAnalyticsDto analytics(LocalDateTime from, LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now();

        List<DiscountRedemption> rows = redemptionRepository.findByRedeemedAtBetween(from, to);
        long applied = rows.stream().filter(r -> r.getStatus() == DiscountRedemptionStatus.APPLIED).count();
        long flagged = rows.stream().filter(r -> Boolean.TRUE.equals(r.getAbuseFlag())).count();
        BigDecimal totalDiscount = redemptionRepository.sumDiscountBetween(from, to);

        Map<UUID, List<DiscountRedemption>> grouped = new HashMap<>();
        for (DiscountRedemption r : rows) {
            grouped.computeIfAbsent(r.getDiscount().getId(), k -> new ArrayList<>()).add(r);
        }

        List<DiscountAnalyticsDto.DiscountSummary> per = new ArrayList<>();
        for (Map.Entry<UUID, List<DiscountRedemption>> e : grouped.entrySet()) {
            List<DiscountRedemption> v = e.getValue();
            BigDecimal sum = v.stream()
                    .filter(r -> r.getStatus() == DiscountRedemptionStatus.APPLIED)
                    .map(DiscountRedemption::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Set<UUID> uniqueCustomers = new HashSet<>();
            for (DiscountRedemption r : v) if (r.getCustomer() != null) uniqueCustomers.add(r.getCustomer().getId());
            per.add(new DiscountAnalyticsDto.DiscountSummary(
                    e.getKey(),
                    v.get(0).getDiscount().getName(),
                    v.stream().filter(r -> r.getStatus() == DiscountRedemptionStatus.APPLIED).count(),
                    sum,
                    uniqueCustomers.size()
            ));
        }
        per.sort(Comparator.comparing(DiscountAnalyticsDto.DiscountSummary::totalDiscount).reversed());

        return new DiscountAnalyticsDto(from, to, applied, flagged, totalDiscount, per);
    }

    // ---------------- mapping ----------------

    private void applyRequest(Discount d, CreateDiscountRequest r) {
        d.setName(r.name());
        d.setDescription(r.description());
        if (r.status() != null) d.setStatus(r.status());
        d.setKind(r.kind());
        d.setValueType(r.valueType());
        d.setValue(r.value());
        d.setMaxDiscountAmount(r.maxDiscountAmount());
        if (r.appliesToScope() != null) d.setAppliesToScope(r.appliesToScope());
        if (r.customerEligibility() != null) d.setCustomerEligibility(r.customerEligibility());
        if (r.minPurchaseType() != null) d.setMinPurchaseType(r.minPurchaseType());
        d.setMinPurchaseAmount(r.minPurchaseAmount());
        d.setMinPurchaseQuantity(r.minPurchaseQuantity());
        d.setUsageLimitTotal(r.usageLimitTotal());
        d.setUsageLimitPerCustomer(r.usageLimitPerCustomer());
        d.setStartsAt(r.startsAt() != null ? r.startsAt() : LocalDateTime.now());
        d.setEndsAt(r.endsAt());
        d.setScheduleDaysOfWeek(r.scheduleDaysOfWeek());
        d.setScheduleStartTime(r.scheduleStartTime());
        d.setScheduleEndTime(r.scheduleEndTime());
        d.setScheduleTimezone(r.scheduleTimezone());
        if (r.stackable() != null) d.setStackable(r.stackable());
        if (r.combineWithOrderDiscounts() != null) d.setCombineWithOrderDiscounts(r.combineWithOrderDiscounts());
        if (r.combineWithProductDiscounts() != null) d.setCombineWithProductDiscounts(r.combineWithProductDiscounts());
        if (r.combineWithShippingDiscounts() != null) d.setCombineWithShippingDiscounts(r.combineWithShippingDiscounts());
        d.setExclusionGroup(r.exclusionGroup());
        if (r.priority() != null) d.setPriority(r.priority());
        if (r.autoApply() != null) d.setAutoApply(r.autoApply());
        if (r.salesChannel() != null) d.setSalesChannel(r.salesChannel());
        d.setBogoBuyQuantity(r.bogoBuyQuantity());
        d.setBogoGetQuantity(r.bogoGetQuantity());
        d.setBogoGetValueType(r.bogoGetValueType());
        d.setBogoGetValue(r.bogoGetValue());
        d.setBundleQuantity(r.bundleQuantity());
        d.setBundlePrice(r.bundlePrice());
        d.setFreeShippingMaxAmount(r.freeShippingMaxAmount());
        d.setFreeShippingCountries(r.freeShippingCountries());

        d.getTiers().clear();
        if (r.tiers() != null) {
            for (DiscountTierDto tier : r.tiers()) {
                DiscountTier t = new DiscountTier();
                t.setDiscount(d);
                t.setMinSubtotal(tier.minSubtotal());
                t.setMinQuantity(tier.minQuantity());
                t.setValueType(tier.valueType());
                t.setValue(tier.value());
                t.setSortOrder(Optional.ofNullable(tier.sortOrder()).orElse(0));
                d.getTiers().add(t);
            }
        }

        d.getProductInclusions().clear();
        if (r.productInclusions() != null) {
            for (DiscountProductInclusionDto p : r.productInclusions()) {
                DiscountProductInclusion inc = new DiscountProductInclusion();
                inc.setDiscount(d);
                inc.setScope(p.scope());
                inc.setEntityId(p.entityId());
                inc.setMode(p.mode() != null ? p.mode() : DiscountInclusionMode.INCLUDE);
                d.getProductInclusions().add(inc);
            }
        }

        d.getCustomerInclusions().clear();
        if (r.customerInclusions() != null) {
            for (DiscountCustomerInclusionDto p : r.customerInclusions()) {
                DiscountCustomerInclusion inc = new DiscountCustomerInclusion();
                inc.setDiscount(d);
                inc.setScope(p.scope());
                inc.setEntityId(p.entityId());
                inc.setMode(p.mode() != null ? p.mode() : DiscountInclusionMode.INCLUDE);
                d.getCustomerInclusions().add(inc);
            }
        }
    }

    private DiscountDto toDto(Discount d) {
        List<DiscountTierDto> tiers = d.getTiers().stream()
                .map(t -> new DiscountTierDto(t.getId(), t.getMinSubtotal(), t.getMinQuantity(),
                        t.getValueType(), t.getValue(), t.getSortOrder()))
                .toList();
        List<DiscountProductInclusionDto> products = d.getProductInclusions().stream()
                .map(p -> new DiscountProductInclusionDto(p.getId(), p.getScope(), p.getEntityId(), p.getMode()))
                .toList();
        List<DiscountCustomerInclusionDto> customers = d.getCustomerInclusions().stream()
                .map(p -> new DiscountCustomerInclusionDto(p.getId(), p.getScope(), p.getEntityId(), p.getMode()))
                .toList();
        List<DiscountCodeDto> codes = discountCodeRepository.findByDiscountId(d.getId()).stream()
                .map(this::toCodeDto).toList();

        return new DiscountDto(
                d.getId(), d.getName(), d.getDescription(), d.getStatus(), d.getKind(),
                d.getValueType(), d.getValue(), d.getMaxDiscountAmount(),
                d.getAppliesToScope(), d.getCustomerEligibility(),
                d.getMinPurchaseType(), d.getMinPurchaseAmount(), d.getMinPurchaseQuantity(),
                d.getUsageLimitTotal(), d.getUsageLimitPerCustomer(), d.getUsedCount(),
                d.getStartsAt(), d.getEndsAt(),
                d.getScheduleDaysOfWeek(), d.getScheduleStartTime(), d.getScheduleEndTime(), d.getScheduleTimezone(),
                d.getStackable(), d.getCombineWithOrderDiscounts(), d.getCombineWithProductDiscounts(),
                d.getCombineWithShippingDiscounts(),
                d.getExclusionGroup(), d.getPriority(), d.getAutoApply(), d.getSalesChannel(),
                d.getBogoBuyQuantity(), d.getBogoGetQuantity(), d.getBogoGetValueType(), d.getBogoGetValue(),
                d.getBundleQuantity(), d.getBundlePrice(),
                d.getFreeShippingMaxAmount(), d.getFreeShippingCountries(),
                tiers, products, customers, codes
        );
    }

    private DiscountCodeDto toCodeDto(DiscountCode c) {
        return new DiscountCodeDto(c.getId(), c.getDiscount().getId(), c.getCode(), c.getStatus(),
                c.getValidFrom(), c.getValidTo(), c.getMaxRedemptions(), c.getMaxRedemptionsPerCustomer(),
                c.getRedeemedCount(), c.getNotes());
    }
}
