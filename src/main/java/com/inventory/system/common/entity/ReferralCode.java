package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "referral_codes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code", "tenant_id"})
})
@Getter
@Setter
public class ReferralCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private ReferralProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReferralCodeStatus status = ReferralCodeStatus.ACTIVE;

    @Column(name = "referees_count", nullable = false)
    private Integer refereesCount = 0;

    @Column(name = "rewards_paid_count", nullable = false)
    private Integer rewardsPaidCount = 0;
}
