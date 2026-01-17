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
@Table(name = "stock_reservations")
@Getter
@Setter
public class StockReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationPriority priority;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(length = 1000)
    private String notes;
}
