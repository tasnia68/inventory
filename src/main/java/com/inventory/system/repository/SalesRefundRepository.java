package com.inventory.system.repository;

import com.inventory.system.common.entity.SalesRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SalesRefundRepository extends JpaRepository<SalesRefund, UUID>, JpaSpecificationExecutor<SalesRefund> {
    boolean existsByRefundNumber(String refundNumber);

    boolean existsByCreditNoteNumber(String creditNoteNumber);
}