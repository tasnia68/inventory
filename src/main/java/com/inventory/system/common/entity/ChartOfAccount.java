package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chart_of_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_code", "tenant_id"})
})
@Getter
@Setter
public class ChartOfAccount extends BaseEntity {

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private ChartOfAccount parentAccount;

    @Column(name = "allow_manual_posting", nullable = false)
    private boolean allowManualPosting = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
