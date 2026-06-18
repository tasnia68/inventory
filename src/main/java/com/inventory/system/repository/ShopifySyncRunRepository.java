package com.inventory.system.repository;

import com.inventory.system.common.entity.ShopifySyncRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopifySyncRunRepository extends JpaRepository<ShopifySyncRun, UUID> {

    List<ShopifySyncRun> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Optional<ShopifySyncRun> findFirstByTenantIdAndSyncTypeAndStatusOrderByCreatedAtDesc(
            String tenantId, String syncType, String status);
}
