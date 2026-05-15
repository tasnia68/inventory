package com.inventory.system.repository;

import com.inventory.system.common.entity.VirtualTryOnAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VirtualTryOnAttemptRepository extends JpaRepository<VirtualTryOnAttempt, UUID> {

    long countByTenantIdAndAttemptedAtAfter(String tenantId, LocalDateTime since);

    long countByTenantIdAndCustomerIdentifierAndAttemptedAtAfter(
            String tenantId, String customerIdentifier, LocalDateTime since);
}
