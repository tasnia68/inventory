package com.inventory.system.repository;

import com.inventory.system.common.entity.SubledgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubledgerEntryRepository extends JpaRepository<SubledgerEntry, UUID> {
    List<SubledgerEntry> findByFinancialEventIdOrderByLineNumberAsc(UUID financialEventId);
}
