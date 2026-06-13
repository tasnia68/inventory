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
@Table(name = "recurring_journal_template_lines")
@Getter
@Setter
public class RecurringJournalTemplateLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private RecurringJournalTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal creditAmount = BigDecimal.ZERO;
}
