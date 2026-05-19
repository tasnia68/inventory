package com.inventory.system.repository;

import com.inventory.system.common.entity.TenantDatasource;
import com.inventory.system.common.entity.TenantDatasourceMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Control-plane repository for the tenant datasource catalog. Not tenant
 * scoped — queries here run on the shared/control connection and intentionally
 * bypass the {@code tenantFilter} (the entity does not extend BaseEntity).
 */
@Repository
public interface TenantDatasourceRepository extends JpaRepository<TenantDatasource, String> {

    List<TenantDatasource> findByMode(TenantDatasourceMode mode);
}
