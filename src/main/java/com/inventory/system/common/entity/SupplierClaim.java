package com.inventory.system.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_claims")
@Getter
@Setter
public class SupplierClaim extends BaseEntity {

    @Column(name = "claim_number", nullable = false, unique = true)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_note_id", nullable = false)
    private GoodsReceiptNote goodsReceiptNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_record_id")
    private DamageRecord damageRecord;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_return_id")
    private SupplierReturn supplierReturn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupplierClaimStatus status = SupplierClaimStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false)
    private SupplierClaimType claimType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "supplierClaim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierClaimItem> items = new ArrayList<>();
}