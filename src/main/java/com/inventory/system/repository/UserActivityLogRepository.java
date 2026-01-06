package com.inventory.system.repository;

import com.inventory.system.common.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {
    List<UserActivityLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
