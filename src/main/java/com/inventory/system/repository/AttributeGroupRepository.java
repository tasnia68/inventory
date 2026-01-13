package com.inventory.system.repository;

import com.inventory.system.common.entity.AttributeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AttributeGroupRepository extends JpaRepository<AttributeGroup, UUID> {
    boolean existsByName(String name);
}
