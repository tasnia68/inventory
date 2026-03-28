package com.inventory.system.repository;

import com.inventory.system.common.entity.StorefrontLoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorefrontLoginChallengeRepository extends JpaRepository<StorefrontLoginChallenge, UUID> {
    List<StorefrontLoginChallenge> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<StorefrontLoginChallenge> findFirstByMagicToken(String magicToken);
}
