package com.inventory.system.repository;

import com.inventory.system.common.entity.ReferralProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferralProgramRepository extends JpaRepository<ReferralProgram, UUID> {
    Optional<ReferralProgram> findFirstByTenantId(String tenantId);
}
