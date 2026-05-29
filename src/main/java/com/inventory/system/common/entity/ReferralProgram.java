package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "referral_programs")
@Getter
@Setter
public class ReferralProgram extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReferralProgramStatus status = ReferralProgramStatus.PAUSED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_discount_id")
    private Discount referrerDiscount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_discount_id")
    private Discount refereeDiscount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_trigger", nullable = false, length = 48)
    private ReferralRewardTrigger rewardTrigger = ReferralRewardTrigger.ON_REFEREE_FIRST_ORDER;

    @Column(name = "min_referee_order_amount", precision = 19, scale = 6)
    private BigDecimal minRefereeOrderAmount;

    @Column(name = "referee_nth_order")
    private Integer refereeNthOrder;

    @Column(name = "max_referrals_per_customer")
    private Integer maxReferralsPerCustomer;

    @Column(columnDefinition = "TEXT")
    private String description;
}
