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
@Table(name = "stock_transaction_items")
@Getter
@Setter
public class StockTransactionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transaction_id", nullable = false)
    private StockTransaction stockTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storage_location_id")
    private StorageLocation sourceStorageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_storage_location_id")
    private StorageLocation destinationStorageLocation;

    @Column(name = "unit_cost", precision = 19, scale = 6)
    private BigDecimal unitCost;
}
