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
@Table(name = "pos_suspended_sale_items")
@Getter
@Setter
public class PosSuspendedSaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_sale_id", nullable = false)
    private PosSuspendedSale suspendedSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "sku_snapshot", nullable = false)
    private String skuSnapshot;

    @Column(name = "description_snapshot", columnDefinition = "TEXT")
    private String descriptionSnapshot;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_discount", nullable = false, precision = 19, scale = 6)
    private BigDecimal lineDiscount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 6)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}