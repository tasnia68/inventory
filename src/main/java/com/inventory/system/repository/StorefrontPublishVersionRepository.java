package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontPublishVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorefrontPublishVersionRepository extends JpaRepository<StorefrontPublishVersion, UUID> {
    List<StorefrontPublishVersion> findAllByOrderByVersionNumberDesc();
    Optional<StorefrontPublishVersion> findTopByOrderByVersionNumberDesc();
}

