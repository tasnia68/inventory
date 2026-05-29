package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "discount_tiers")
@Getter
@Setter
public class DiscountTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "min_subtotal", precision = 19, scale = 6)
    private BigDecimal minSubtotal;

    @Column(name = "min_quantity", precision = 19, scale = 6)
    private BigDecimal minQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 32)
    private DiscountValueType valueType;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal value;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
