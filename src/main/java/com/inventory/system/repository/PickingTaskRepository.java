package com.inventory.system.repository;

import com.inventory.system.common.entity.PickingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PickingTaskRepository extends JpaRepository<PickingTask, UUID>, JpaSpecificationExecutor<PickingTask> {
}
