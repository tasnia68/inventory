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
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_attributions")
@Getter
@Setter
public class ReferralAttribution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_code_id", nullable = false)
    private ReferralCode referralCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_customer_id", nullable = false)
    private Customer refereeCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_order_id")
    private SalesOrder refereeOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReferralAttributionStatus status = ReferralAttributionStatus.PENDING;

    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    @Column(name = "referrer_reward_amount", precision = 19, scale = 6)
    private BigDecimal referrerRewardAmount;

    @Column(name = "referee_reward_amount", precision = 19, scale = 6)
    private BigDecimal refereeRewardAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
