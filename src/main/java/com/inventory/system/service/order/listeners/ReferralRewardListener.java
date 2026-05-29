package com.inventory.system.service.order.listeners;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.DiscountValueType;
import com.inventory.system.common.entity.GiftCardSource;
import com.inventory.system.common.entity.ReferralAttribution;
import com.inventory.system.common.entity.ReferralAttributionStatus;
import com.inventory.system.common.entity.ReferralCode;
import com.inventory.system.common.entity.ReferralCodeStatus;
import com.inventory.system.common.entity.ReferralProgram;
import com.inventory.system.common.entity.ReferralProgramStatus;
import com.inventory.system.common.entity.ReferralRewardTrigger;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.payload.IssueGiftCardRequest;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.ReferralAttributionRepository;
import com.inventory.system.repository.ReferralCodeRepository;
import com.inventory.system.repository.ReferralProgramRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.service.GiftCardService;
import com.inventory.system.service.order.events.SalesOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Promotes a referee's PENDING attribution to QUALIFIED → REWARDED when their order satisfies the
 * configured reward trigger, and issues a gift card to the referrer worth the referrerDiscount's value.
 *
 * Also handles the case where the referee enters the referral code at checkout (event carries it):
 * creates the attribution if one doesn't already exist, then evaluates the trigger.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReferralRewardListener {

    private final ReferralProgramRepository programRepository;
    private final ReferralCodeRepository codeRepository;
    private final ReferralAttributionRepository attributionRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final GiftCardService giftCardService;

    @Async
    @EventListener
    @Transactional
    public void onSalesOrderCreated(SalesOrderCreatedEvent event) {
        try {
            UUID customerId = event.customerId();
            if (customerId == null) return;

            Optional<ReferralProgram> programOpt = programRepository.findAll().stream().findFirst();
            if (programOpt.isEmpty() || programOpt.get().getStatus() != ReferralProgramStatus.ACTIVE) return;
            ReferralProgram program = programOpt.get();

            ReferralAttribution attribution = resolveAttribution(event, program, customerId);
            if (attribution == null) return;
            if (attribution.getStatus() == ReferralAttributionStatus.REWARDED
                    || attribution.getStatus() == ReferralAttributionStatus.REJECTED) return;

            SalesOrder order = salesOrderRepository.findById(event.salesOrderId()).orElse(null);
            if (order == null) return;

            if (!triggerSatisfied(program, customerId, order)) return;

            attribution.setStatus(ReferralAttributionStatus.QUALIFIED);
            attribution.setQualifiedAt(LocalDateTime.now());
            attribution.setRefereeOrder(order);
            attributionRepository.save(attribution);

            issueReferrerReward(program, attribution);
            issueRefereeReward(program, attribution);

            attribution.setStatus(ReferralAttributionStatus.REWARDED);
            attribution.setRewardedAt(LocalDateTime.now());
            attributionRepository.save(attribution);

            ReferralCode code = attribution.getReferralCode();
            code.setRewardsPaidCount((code.getRewardsPaidCount() == null ? 0 : code.getRewardsPaidCount()) + 1);
            codeRepository.save(code);
        } catch (Exception e) {
            log.warn("Referral attribution failed for order {}: {}", event.salesOrderId(), e.getMessage());
        }
    }

    private ReferralAttribution resolveAttribution(SalesOrderCreatedEvent event, ReferralProgram program, UUID refereeId) {
        // 1. Existing attribution wins
        Optional<ReferralAttribution> existing = attributionRepository.findByRefereeCustomerId(refereeId);
        if (existing.isPresent()) return existing.get();

        // 2. Referee may have entered the code at checkout — create attribution now
        String enteredCode = event.referralCode();
        if (enteredCode == null || enteredCode.isBlank()) return null;
        ReferralCode rc = codeRepository.findByCodeIgnoreCase(enteredCode.trim()).orElse(null);
        if (rc == null || rc.getStatus() != ReferralCodeStatus.ACTIVE) return null;
        if (rc.getCustomer().getId().equals(refereeId)) return null; // can't self-refer
        if (program.getMaxReferralsPerCustomer() != null
                && rc.getRefereesCount() != null
                && rc.getRefereesCount() >= program.getMaxReferralsPerCustomer()) return null;

        Customer referee = customerRepository.findById(refereeId).orElse(null);
        if (referee == null) return null;

        ReferralAttribution attribution = new ReferralAttribution();
        attribution.setReferralCode(rc);
        attribution.setRefereeCustomer(referee);
        attribution.setStatus(ReferralAttributionStatus.PENDING);
        attribution = attributionRepository.save(attribution);

        rc.setRefereesCount((rc.getRefereesCount() == null ? 0 : rc.getRefereesCount()) + 1);
        codeRepository.save(rc);

        return attribution;
    }

    private boolean triggerSatisfied(ReferralProgram program, UUID refereeId, SalesOrder order) {
        BigDecimal min = program.getMinRefereeOrderAmount();
        if (min != null && order.getTotalAmount() != null && order.getTotalAmount().compareTo(min) < 0) {
            return false;
        }
        ReferralRewardTrigger trigger = program.getRewardTrigger() != null
                ? program.getRewardTrigger() : ReferralRewardTrigger.ON_REFEREE_FIRST_ORDER;

        long orderCount = salesOrderRepository.countByCustomerId(refereeId);
        return switch (trigger) {
            case ON_REFEREE_SIGNUP -> true; // signup happened before order; presence of any order qualifies
            case ON_REFEREE_FIRST_ORDER -> orderCount == 1; // this very order
            case ON_REFEREE_NTH_ORDER -> {
                Integer n = program.getRefereeNthOrder();
                yield n != null && orderCount == n.longValue();
            }
        };
    }

    private void issueReferrerReward(ReferralProgram program, ReferralAttribution attribution) {
        Discount referrer = program.getReferrerDiscount();
        if (referrer == null) return;
        BigDecimal value = computeRewardAmount(referrer, attribution.getRefereeOrder());
        if (value == null || value.signum() <= 0) return;
        Customer referrerCustomer = attribution.getReferralCode().getCustomer();
        if (referrerCustomer == null) return;
        issueRewardGiftCard(referrerCustomer.getId(), value, "Referral reward (referrer): " + referrer.getName());
        attribution.setReferrerRewardAmount(value);
    }

    private void issueRefereeReward(ReferralProgram program, ReferralAttribution attribution) {
        Discount referee = program.getRefereeDiscount();
        if (referee == null) return;
        BigDecimal value = computeRewardAmount(referee, attribution.getRefereeOrder());
        if (value == null || value.signum() <= 0) return;
        issueRewardGiftCard(attribution.getRefereeCustomer().getId(), value, "Referral reward (referee): " + referee.getName());
        attribution.setRefereeRewardAmount(value);
    }

    private BigDecimal computeRewardAmount(Discount discount, SalesOrder order) {
        // For REFERRAL_* discounts the value field carries the reward amount or percentage.
        // For other kinds we still use value as a flat reward.
        if (discount.getValue() == null) return null;
        if (discount.getValueType() == DiscountValueType.PERCENTAGE
                && order != null && order.getTotalAmount() != null) {
            return order.getTotalAmount().multiply(discount.getValue())
                    .divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP);
        }
        // Fall back: treat value as fixed amount regardless of valueType
        return discount.getValue();
    }

    private void issueRewardGiftCard(UUID customerId, BigDecimal value, String note) {
        IssueGiftCardRequest req = new IssueGiftCardRequest(
                null,                       // server-generated code
                "BDT",
                value,
                customerId,
                LocalDateTime.now().plusYears(1),
                GiftCardSource.REFERRAL_REWARD,
                note
        );
        giftCardService.issue(req);
    }
}
