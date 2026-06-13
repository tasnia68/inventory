package com.inventory.system.repository;

import com.inventory.system.common.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TaxRateRepository extends JpaRepository<TaxRate, UUID>, JpaSpecificationExecutor<TaxRate> {
    Optional<TaxRate> findByCode(String code);
}
