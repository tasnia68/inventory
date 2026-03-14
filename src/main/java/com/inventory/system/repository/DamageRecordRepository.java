package com.inventory.system.repository;

import com.inventory.system.common.entity.DamageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DamageRecordRepository extends JpaRepository<DamageRecord, UUID>, JpaSpecificationExecutor<DamageRecord> {
}