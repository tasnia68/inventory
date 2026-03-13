package com.inventory.system.repository;

import com.inventory.system.common.entity.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID> {
    List<SupplierDocument> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
}