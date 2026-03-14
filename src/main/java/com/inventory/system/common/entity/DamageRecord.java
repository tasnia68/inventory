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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "damage_records")
@Getter
@Setter
public class DamageRecord extends BaseEntity {

    @Column(name = "record_number", nullable = false, unique = true)
    private String recordNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DamageRecordStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private DamageRecordSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    private DamageReasonCode reasonCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_location_id")
    private StorageLocation quarantineLocation;

    @Column(name = "reference")
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "damage_date", nullable = false)
    private LocalDateTime damageDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "damageRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DamageRecordItem> items = new ArrayList<>();
}