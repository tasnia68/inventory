package com.inventory.system.controller;

import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookEvent;
import com.inventory.system.common.entity.InboundWebhookStatus;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.InboundWebhookEventDto;
import com.inventory.system.payload.MaterializeWebhookRequest;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.repository.InboundWebhookEventRepository;
import com.inventory.system.service.ingestion.ExternalOrderIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook-events")
@RequiredArgsConstructor
public class InboundWebhookEventController {

    private final InboundWebhookEventRepository repository;
    private final ExternalOrderIngestionService ingestionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InboundWebhookEventDto>>> list(
            @RequestParam(required = false) InboundWebhookStatus status,
            @RequestParam(required = false) ExternalOrderSource source) {
        List<InboundWebhookEventDto> result = repository.findAll().stream()
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> source == null || e.getSource() == source)
                .sorted((a, b) -> b.getReceivedAt().compareTo(a.getReceivedAt()))
                .limit(200)
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InboundWebhookEventDto>> get(@PathVariable UUID id) {
        InboundWebhookEvent event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InboundWebhookEvent", "id", id));
        return ResponseEntity.ok(ApiResponse.success(toDto(event), "OK"));
    }

    @PostMapping("/{id}/materialize")
    public ResponseEntity<ApiResponse<SalesOrderDto>> materialize(@PathVariable UUID id, @Valid @RequestBody MaterializeWebhookRequest request) {
        SalesOrderDto created = ingestionService.materialize(id, request);
        return ResponseEntity.ok(ApiResponse.success(created, "Sales order created from webhook event"));
    }

    private InboundWebhookEventDto toDto(InboundWebhookEvent e) {
        return new InboundWebhookEventDto(
                e.getId(), e.getSource(), e.getExternalEventId(), e.getTopic(),
                e.getStatus(), e.getError(), e.getSalesOrderId(),
                e.getReceivedAt(), e.getProcessedAt(), e.getPayload()
        );
    }
}
