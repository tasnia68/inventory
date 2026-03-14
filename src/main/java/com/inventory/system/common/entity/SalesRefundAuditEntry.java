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

import java.time.LocalDateTime;

@Entity
@Table(name = "sales_refund_audit_entries")
@Getter
@Setter
public class SalesRefundAuditEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_refund_id", nullable = false)
    private SalesRefund salesRefund;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private SalesRefundAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private SalesRefundStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private SalesRefundStatus toStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;
}