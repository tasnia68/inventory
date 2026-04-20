package com.inventory.system.repository;

import com.inventory.system.common.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndTenantId(String email, String tenantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update User u set u.tenantId = :tenantId where u.id = :userId")
    int updateTenantIdById(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    boolean existsByEmail(String email);
    boolean existsByEmailAndTenantId(String email, String tenantId);
}
