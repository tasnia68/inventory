package com.inventory.system.controller.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.service.TenantSettingService;
import com.inventory.system.service.courier.CourierWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Steadfast delivery-status callbacks per tenant. Authenticated with a
 * Bearer token stored in tenant setting {@code steadfast.webhook_token}. Replaces
 * the manual "paste the tracking link" step with hands-free status + email updates.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/steadfast/{tenantId}")
@RequiredArgsConstructor
public class SteadfastWebhookController {

    private static final String WEBHOOK_TOKEN_KEY = "steadfast.webhook_token";

    private final TenantSettingService tenantSettingService;
    private final CourierWebhookService courierWebhookService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> handle(
            @PathVariable String tenantId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody byte[] rawBody) {
        return TenantContext.callWithTenant(tenantId, () -> {
            String token = tenantSettingService.findSetting(WEBHOOK_TOKEN_KEY).map(s -> s.getValue()).orElse(null);
            if (token == null || token.isBlank()) {
                log.warn("Steadfast webhook received but {} not configured for tenant {}", WEBHOOK_TOKEN_KEY, tenantId);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.<String>builder().success(false).status(503)
                                .message("Steadfast webhook token not configured").build());
            }
            if (authorization == null || !authorization.equals("Bearer " + token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<String>builder().success(false).status(401)
                                .message("Invalid Steadfast webhook token").build());
            }
            try {
                JsonNode node = objectMapper.readTree(rawBody);
                String consignmentId = textOrNull(node, "consignment_id");
                String trackingCode = textOrNull(node, "tracking_code");
                String status = node.hasNonNull("delivery_status")
                        ? node.get("delivery_status").asText()
                        : textOrNull(node, "status");
                boolean applied = courierWebhookService.applySteadfastStatus(consignmentId, trackingCode, status);
                return ResponseEntity.ok(ApiResponse.success(
                        applied ? "applied" : "ignored",
                        applied ? "Shipment updated" : "No matching shipment"));
            } catch (Exception e) {
                log.error("Steadfast webhook processing failed for tenant {}: {}", tenantId, e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.<String>builder().success(false).status(400)
                                .message("Invalid Steadfast webhook payload").build());
            }
        });
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
