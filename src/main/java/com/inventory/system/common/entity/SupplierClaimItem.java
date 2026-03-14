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
@Table(name = "supplier_claim_items")
@Getter
@Setter
public class SupplierClaimItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_claim_id", nullable = false)
    private SupplierClaim supplierClaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_note_item_id", nullable = false)
    private GoodsReceiptNoteItem goodsReceiptNoteItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "claimed_amount", precision = 19, scale = 6)
    private BigDecimal claimedAmount;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}