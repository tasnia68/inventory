package com.inventory.system.service;

import com.inventory.system.common.entity.AccountingPeriod;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.repository.AccountingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountingPeriodService {

    private final AccountingPeriodRepository repository;

    @Transactional(readOnly = true)
    public List<AccountingPeriod> list() {
        return repository.findAllByOrderByPeriodStartDesc();
    }

    @Transactional
    public AccountingPeriod close(LocalDate periodStart, LocalDate periodEnd, String notes, String closedBy) {
        if (periodStart == null || periodEnd == null) {
            throw new BadRequestException("Both periodStart and periodEnd are required");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new BadRequestException("periodEnd must be on or after periodStart");
        }
        AccountingPeriod period = new AccountingPeriod();
        period.setPeriodStart(periodStart);
        period.setPeriodEnd(periodEnd);
        period.setStatus(AccountingPeriod.Status.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(closedBy);
        period.setNotes(notes);
        return repository.save(period);
    }

    @Transactional
    public AccountingPeriod reopen(UUID periodId, String reopenedBy) {
        AccountingPeriod period = repository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountingPeriod", "id", periodId));
        if (period.getStatus() == AccountingPeriod.Status.OPEN) {
            return period;
        }
        period.setStatus(AccountingPeriod.Status.OPEN);
        period.setReopenedAt(LocalDateTime.now());
        period.setReopenedBy(reopenedBy);
        return repository.save(period);
    }

    /**
     * Throws if the given entry date falls inside a closed accounting period.
     * Called from every journal-entry save site so backdated edits to closed
     * periods are rejected uniformly.
     */
    @Transactional(readOnly = true)
    public void assertOpen(LocalDate entryDate) {
        if (entryDate == null) return;
        repository.findClosedPeriodCovering(entryDate).ifPresent(period -> {
            throw new BadRequestException(
                    "Accounting period " + period.getPeriodStart() + " to " + period.getPeriodEnd()
                            + " is closed; cannot post or modify entries dated " + entryDate);
        });
    }

    public void assertOpen(LocalDateTime entryDateTime) {
        if (entryDateTime != null) assertOpen(entryDateTime.toLocalDate());
    }
}
