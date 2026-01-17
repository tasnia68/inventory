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
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_valuation_layers")
@Getter
@Setter
public class InventoryValuationLayer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_remaining", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityRemaining;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "reference")
    private String reference;
}
