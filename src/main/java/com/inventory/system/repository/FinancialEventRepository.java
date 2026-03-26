package com.inventory.system.repository;

import com.inventory.system.common.entity.FinancialEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface FinancialEventRepository extends JpaRepository<FinancialEvent, UUID>, JpaSpecificationExecutor<FinancialEvent> {
    Optional<FinancialEvent> findBySourceDocumentTypeAndSourceDocumentId(String sourceDocumentType, String sourceDocumentId);
}
