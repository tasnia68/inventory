package com.inventory.system.repository;

import com.inventory.system.common.entity.DataImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataImportHistoryRepository extends JpaRepository<DataImportHistory, UUID> {
    List<DataImportHistory> findTop100ByOrderByCreatedAtDesc();
}