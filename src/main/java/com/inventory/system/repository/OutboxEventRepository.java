package com.inventory.system.repository;

import com.inventory.system.common.entity.OutboxEvent;
import com.inventory.system.common.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("select e from OutboxEvent e where e.status = :status "
            + "and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) order by e.createdAt asc")
    List<OutboxEvent> findDispatchable(@Param("status") OutboxStatus status,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);
}
