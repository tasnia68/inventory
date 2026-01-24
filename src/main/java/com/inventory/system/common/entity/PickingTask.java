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
@Table(name = "picking_tasks")
@Getter
@Setter
public class PickingTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_list_id", nullable = false)
    private PickingList pickingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_item_id", nullable = false)
    private SalesOrderItem salesOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "requested_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal requestedQuantity;

    @Column(name = "picked_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal pickedQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickingStatus status = PickingStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
