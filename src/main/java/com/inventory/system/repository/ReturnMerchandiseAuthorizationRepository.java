package com.inventory.system.repository;

import com.inventory.system.common.entity.ReturnMerchandiseAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReturnMerchandiseAuthorizationRepository extends JpaRepository<ReturnMerchandiseAuthorization, UUID>, JpaSpecificationExecutor<ReturnMerchandiseAuthorization> {
    boolean existsByRmaNumber(String rmaNumber);
}