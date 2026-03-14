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
@Table(name = "sales_refund_items")
@Getter
@Setter
public class SalesRefundItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_refund_id", nullable = false)
    private SalesRefund salesRefund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_item_id", nullable = false)
    private SalesOrderItem salesOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_disposition", nullable = false)
    private ReturnDisposition returnDisposition;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "serial_numbers", columnDefinition = "TEXT")
    private String serialNumbers;
}