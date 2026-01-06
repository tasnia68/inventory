package com.inventory.system.repository;

import com.inventory.system.common.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {
    Optional<UserInvitation> findByToken(String token);
    Optional<UserInvitation> findByEmail(String email);
}
