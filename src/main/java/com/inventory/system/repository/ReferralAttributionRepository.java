package com.inventory.system.repository;

import com.inventory.system.common.entity.ReferralAttribution;
import com.inventory.system.common.entity.ReferralAttributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralAttributionRepository extends JpaRepository<ReferralAttribution, UUID> {
    Optional<ReferralAttribution> findByRefereeCustomerId(UUID refereeCustomerId);

    List<ReferralAttribution> findByReferralCodeIdAndStatus(UUID referralCodeId, ReferralAttributionStatus status);

    long countByReferralCodeId(UUID referralCodeId);

    List<ReferralAttribution> findByReferralCodeId(UUID referralCodeId);
}
