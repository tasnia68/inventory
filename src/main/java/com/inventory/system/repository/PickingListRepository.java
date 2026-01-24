package com.inventory.system.repository;

import com.inventory.system.common.entity.PickingList;
import com.inventory.system.common.entity.PickingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PickingListRepository extends JpaRepository<PickingList, UUID>, JpaSpecificationExecutor<PickingList> {
    Page<PickingList> findByWarehouseId(UUID warehouseId, Pageable pageable);
    Page<PickingList> findByStatus(PickingStatus status, Pageable pageable);
}
