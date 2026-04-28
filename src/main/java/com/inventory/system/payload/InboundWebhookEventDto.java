package com.inventory.system.payload;

import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundWebhookEventDto {
    private UUID id;
    private ExternalOrderSource source;
    private String externalEventId;
    private String topic;
    private InboundWebhookStatus status;
    private String error;
    private UUID salesOrderId;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String payload;
}
