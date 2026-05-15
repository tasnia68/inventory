package com.inventory.system.repository;

import com.inventory.system.common.entity.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, UUID> {

    List<AccountingPeriod> findAllByOrderByPeriodStartDesc();

    Optional<AccountingPeriod> findFirstByStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            AccountingPeriod.Status status, LocalDate startBound, LocalDate endBound);

    default Optional<AccountingPeriod> findClosedPeriodCovering(LocalDate date) {
        return findFirstByStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                AccountingPeriod.Status.CLOSED, date, date);
    }
}
