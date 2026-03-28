package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontAccountRepository extends JpaRepository<StorefrontAccount, UUID> {
    Optional<StorefrontAccount> findFirstByEmailIgnoreCase(String email);
    Optional<StorefrontAccount> findFirstByCustomerId(UUID customerId);
}
