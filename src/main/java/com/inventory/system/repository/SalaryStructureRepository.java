package com.inventory.system.repository;

import com.inventory.system.common.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {
}
