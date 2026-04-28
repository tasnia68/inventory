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
@Table(name = "shipping_rate_cards")
@Getter
@Setter
public class ShippingRateCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_profile_id", nullable = false)
    private CourierProfile courierProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false, length = 32)
    private DeliveryZone zone;

    @Column(name = "customer_charge", nullable = false, precision = 19, scale = 6)
    private BigDecimal customerCharge;

    @Column(name = "courier_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal courierCost;

    @Column(name = "cod_fee_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal codFeePercent = BigDecimal.ZERO;

    @Column(name = "weight_kg_included", precision = 7, scale = 3)
    private BigDecimal weightKgIncluded;

    @Column(name = "per_kg_overage", precision = 19, scale = 6)
    private BigDecimal perKgOverage;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
}
