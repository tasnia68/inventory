package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontAccountSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontAccountSessionRepository extends JpaRepository<StorefrontAccountSession, UUID> {
    Optional<StorefrontAccountSession> findFirstBySessionToken(String sessionToken);
    List<StorefrontAccountSession> findByStorefrontAccountId(UUID storefrontAccountId);
}
