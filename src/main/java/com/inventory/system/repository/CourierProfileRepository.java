package com.inventory.system.repository;

import com.inventory.system.common.entity.CourierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourierProfileRepository extends JpaRepository<CourierProfile, UUID>, JpaSpecificationExecutor<CourierProfile> {

    List<CourierProfile> findByIsActiveTrue();

    Optional<CourierProfile> findFirstByProviderCodeIgnoreCaseAndIsActiveTrue(String providerCode);

    Optional<CourierProfile> findFirstByIsDefaultTrueAndIsActiveTrue();
}
