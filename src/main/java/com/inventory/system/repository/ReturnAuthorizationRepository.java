package com.inventory.system.repository;

import com.inventory.system.common.entity.ReturnAuthorization;
import com.inventory.system.common.entity.RmaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnAuthorizationRepository extends JpaRepository<ReturnAuthorization, UUID>, JpaSpecificationExecutor<ReturnAuthorization> {
    boolean existsByRmaNumber(String rmaNumber);
    List<ReturnAuthorization> findBySalesOrderId(UUID salesOrderId);
    List<ReturnAuthorization> findByStatus(RmaStatus status);
}
