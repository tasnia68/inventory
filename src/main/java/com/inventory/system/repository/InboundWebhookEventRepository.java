package com.inventory.system.repository;

import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboundWebhookEventRepository extends JpaRepository<InboundWebhookEvent, UUID> {

    Optional<InboundWebhookEvent> findFirstBySourceAndExternalEventId(ExternalOrderSource source, String externalEventId);
}
