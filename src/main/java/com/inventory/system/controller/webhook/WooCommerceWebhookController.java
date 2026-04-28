package com.inventory.system.controller.webhook;

import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookStatus;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.service.TenantSettingService;
import com.inventory.system.service.ingestion.ExternalOrderIngestionService;
import com.inventory.system.service.ingestion.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/woocommerce/{tenantId}")
@RequiredArgsConstructor
public class WooCommerceWebhookController {

    private static final String WOO_SIGNATURE_HEADER = "X-WC-Webhook-Signature";
    private static final String WOO_TOPIC_HEADER = "X-WC-Webhook-Topic";
    private static final String SECRET_KEY = "woocommerce.webhook_secret";

    private final ExternalOrderIngestionService ingestionService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final TenantSettingService tenantSettingService;

    @PostMapping(value = "/orders", consumes = "application/json")
    public ResponseEntity<ApiResponse<ExternalOrderIngestionService.IngestionResult>> orders(
            @PathVariable String tenantId,
            @RequestHeader(value = WOO_SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = WOO_TOPIC_HEADER, required = false) String topic,
            @RequestBody byte[] rawBody) {
        String secret = tenantSettingService.findSetting(SECRET_KEY).map(s -> s.getValue()).orElse(null);
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<ExternalOrderIngestionService.IngestionResult>builder()
                            .success(false).status(503).message("WooCommerce webhook secret not configured").build());
        }
        if (!signatureVerifier.verifyHmacSha256(rawBody, signature, secret, WebhookSignatureVerifier.Encoding.BASE64)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<ExternalOrderIngestionService.IngestionResult>builder()
                            .success(false).status(401).message("Invalid WooCommerce webhook signature").build());
        }
        String payload = new String(rawBody, StandardCharsets.UTF_8);
        ExternalOrderIngestionService.IngestionResult result = ingestionService.ingest(
                ExternalOrderSource.WOOCOMMERCE, topic, payload, signature);
        HttpStatus responseStatus = result.status() == InboundWebhookStatus.FAILED ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(responseStatus).body(ApiResponse.success(result, result.status().name()));
    }
}
