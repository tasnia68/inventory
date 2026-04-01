package com.inventory.system.repository;

import com.inventory.system.common.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {
    List<PayrollRun> findAllByOrderByCreatedAtDesc();
}
