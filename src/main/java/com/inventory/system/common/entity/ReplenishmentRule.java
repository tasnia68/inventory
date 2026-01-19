package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "replenishment_rules")
@Getter
@Setter
public class ReplenishmentRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "min_stock", precision = 19, scale = 6, nullable = false)
    private BigDecimal minStock; // Reorder Point

    @Column(name = "max_stock", precision = 19, scale = 6, nullable = false)
    private BigDecimal maxStock; // Target Stock

    @Column(name = "reorder_quantity", precision = 19, scale = 6)
    private BigDecimal reorderQuantity;

    @Column(name = "safety_stock", precision = 19, scale = 6)
    private BigDecimal safetyStock;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
}
