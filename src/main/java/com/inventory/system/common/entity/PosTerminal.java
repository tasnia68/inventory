package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pos_terminals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"terminal_code", "tenant_id"})
})
@Getter
@Setter
public class PosTerminal extends BaseEntity {

    @Column(name = "terminal_code", nullable = false)
    private String terminalCode;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;
}