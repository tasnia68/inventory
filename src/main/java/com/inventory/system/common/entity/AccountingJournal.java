package com.inventory.system.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounting_journals", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"journal_code", "tenant_id"})
})
@Getter
@Setter
public class AccountingJournal extends BaseEntity {

    @Column(name = "journal_code", nullable = false)
    private String journalCode;

    @Column(name = "journal_name", nullable = false)
    private String journalName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "system_journal", nullable = false)
    private boolean systemJournal = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
