package com.inventory.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.ProductImage;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.TenantVirtualTryOnSettings;
import com.inventory.system.common.entity.VirtualTryOnAttempt;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.TenantVirtualTryOnSettingsRepository;
import com.inventory.system.repository.VirtualTryOnAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualTryOnService {

    /** Platform tenant id ("platform" subdomain) — where the shared Gemini key is stored. */
    private static final String PLATFORM_GEMINI_KEY_SETTING = "platform.gemini_api_key";
    private static final String PLATFORM_GEMINI_MODEL_SETTING = "platform.gemini_model";
    private static final String DEFAULT_GEMINI_MODEL = "gemini-3.1-flash-image-preview";
    private static final String GEMINI_ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final TenantVirtualTryOnSettingsRepository settingsRepository;
    private final VirtualTryOnAttemptRepository attemptRepository;
    private final ProductVariantRepository productVariantRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public record TryOnResult(String imageBase64, String mimeType) {}

    @Transactional
    public TryOnResult requestTryOn(String tenantId, String customerIdentifier, UUID variantId, String userImageBase64) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Tenant could not be resolved from request");
        }
        if (variantId == null) {
            throw new BadRequestException("productVariantId is required");
        }
        if (userImageBase64 == null || userImageBase64.isBlank()) {
            throw new BadRequestException("userImageBase64 is required");
        }
        String identifier = (customerIdentifier == null || customerIdentifier.isBlank())
                ? "guest" : customerIdentifier.trim().toLowerCase();

        TenantVirtualTryOnSettings settings = settingsRepository.findById(tenantId)
                .orElseThrow(() -> new BadRequestException("Virtual try-on is not enabled for this tenant"));
        if (!settings.isEnabled()) {
            throw new BadRequestException("Virtual try-on is not enabled for this tenant");
        }

        // Quota: per-customer per day (rolling 24h)
        long perCustomer = attemptRepository.countByTenantIdAndCustomerIdentifierAndAttemptedAtAfter(
                tenantId, identifier, LocalDateTime.now().minusDays(1));
        if (perCustomer >= settings.getMaxPerCustomerPerDay()) {
            recordFailure(tenantId, identifier, variantId,
                    "Customer daily quota reached (" + settings.getMaxPerCustomerPerDay() + ")");
            throw new BadRequestException("Daily try-on limit reached for this customer. Please try again tomorrow.");
        }
        // Quota: per-tenant per month (rolling 30d)
        long perTenant = attemptRepository.countByTenantIdAndAttemptedAtAfter(
                tenantId, LocalDateTime.now().minusDays(30));
        if (perTenant >= settings.getMaxPerTenantPerMonth()) {
            recordFailure(tenantId, identifier, variantId,
                    "Tenant monthly quota reached (" + settings.getMaxPerTenantPerMonth() + ")");
            throw new BadRequestException("This store has reached its monthly try-on limit. Please contact the operator.");
        }

        // Resolve product image — server-side, bypasses storefront auth.
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));
        ProductImage primaryImage = variant.getTemplate().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .or(() -> variant.getTemplate().getImages().stream().findFirst())
                .orElseThrow(() -> new BadRequestException("Product has no images for try-on"));

        String productImageBase64 = readProductImageAsBase64(primaryImage);

        String apiKey = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_KEY_SETTING)
                .filter(v -> v != null && !v.isBlank())
                .orElseThrow(() -> new BadRequestException(
                        "Virtual try-on is misconfigured: platform Gemini API key is not set. Ask your administrator."));

        String model = tenantSettingRepository.findValueAcrossTenantsBySettingKey(PLATFORM_GEMINI_MODEL_SETTING)
                .filter(v -> v != null && !v.isBlank())
                .orElse(DEFAULT_GEMINI_MODEL);

        try {
            TryOnResult result = callGemini(apiKey, model, stripDataUri(userImageBase64), productImageBase64);
            recordAttempt(tenantId, identifier, variantId, true, null);
            return result;
        } catch (Exception e) {
            log.warn("Virtual try-on Gemini call failed for tenant {}: {}", tenantId, e.getMessage());
            recordFailure(tenantId, identifier, variantId, e.getMessage());
            throw new BadRequestException("Virtual try-on generation failed: " + e.getMessage());
        }
    }

    private String readProductImageAsBase64(ProductImage img) {
        String storageKey = img.getUrl();
        if (storageKey == null || storageKey.isBlank()) {
            throw new BadRequestException("Product image has no storage key");
        }
        try (InputStream in = fileStorageService.getFile(storageKey);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (java.io.IOException e) {
            throw new BadRequestException("Could not read product image from storage: " + e.getMessage());
        }
    }

    private TryOnResult callGemini(String apiKey, String model, String userBase64, String productBase64) throws Exception {
        String body = """
                {
                  "contents": [{
                    "role": "user",
                    "parts": [
                      {"text": "Virtual try-on. Image 1 is the person. Image 2 is the garment. Generate a high-fidelity image of the person from Image 1 wearing the garment from Image 2. Preserve their facial identity and body shape. Preserve the exact fabric texture, pattern, and colour of the garment."},
                      {"inline_data": {"mime_type": "image/jpeg", "data": "%s"}},
                      {"inline_data": {"mime_type": "image/jpeg", "data": "%s"}}
                    ]
                  }],
                  "generationConfig": {"responseModalities": ["IMAGE"]}
                }
                """.formatted(userBase64, productBase64);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(GEMINI_ENDPOINT_TEMPLATE, model, apiKey)))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new BadRequestException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidate = root.path("candidates").path(0);
        JsonNode parts = candidate.path("content").path("parts");
        StringBuilder textParts = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode inline = part.has("inline_data") ? part.path("inline_data") : part.path("inlineData");
            if (!inline.isMissingNode() && inline.has("data")) {
                String mime = inline.path("mime_type").asText(inline.path("mimeType").asText("image/jpeg"));
                return new TryOnResult(inline.path("data").asText(), mime);
            }
            if (part.has("text")) {
                textParts.append(part.path("text").asText()).append(' ');
            }
        }
        // No image — surface the real reason (safety block, text-only reply, prompt feedback).
        String finishReason = candidate.path("finishReason").asText("");
        String blockReason = root.path("promptFeedback").path("blockReason").asText("");
        StringBuilder reason = new StringBuilder("Gemini returned no image");
        if (!finishReason.isBlank()) reason.append("; finishReason=").append(finishReason);
        if (!blockReason.isBlank()) reason.append("; blockReason=").append(blockReason);
        if (textParts.length() > 0) {
            String t = textParts.toString().trim();
            reason.append("; model said: ").append(t.length() > 300 ? t.substring(0, 300) + "…" : t);
        }
        log.warn("Virtual try-on no-image response: {}", response.body().length() > 800
                ? response.body().substring(0, 800) : response.body());
        throw new BadRequestException(reason.toString());
    }

    private static String stripDataUri(String value) {
        if (value == null) return null;
        int comma = value.indexOf(',');
        if (value.startsWith("data:") && comma > 0) {
            return value.substring(comma + 1);
        }
        return value;
    }

    private void recordAttempt(String tenantId, String customerIdentifier, UUID variantId, boolean success, String errorMessage) {
        VirtualTryOnAttempt attempt = new VirtualTryOnAttempt();
        attempt.setTenantId(tenantId);
        attempt.setCustomerIdentifier(customerIdentifier);
        attempt.setProductVariantId(variantId);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setSuccess(success);
        attempt.setErrorMessage(errorMessage);
        attemptRepository.save(attempt);
    }

    private void recordFailure(String tenantId, String customerIdentifier, UUID variantId, String error) {
        recordAttempt(tenantId, customerIdentifier, variantId, false, error);
    }

    /* ---------- Super-admin API ---------- */

    @Transactional(readOnly = true)
    public Optional<TenantVirtualTryOnSettings> getTenantSettings(String tenantId) {
        return settingsRepository.findById(tenantId);
    }

    @Transactional
    public TenantVirtualTryOnSettings upsertTenantSettings(String tenantId, boolean enabled,
                                                            int maxPerCustomerPerDay, int maxPerTenantPerMonth) {
        TenantVirtualTryOnSettings settings = settingsRepository.findById(tenantId)
                .orElseGet(TenantVirtualTryOnSettings::new);
        settings.setTenantId(tenantId);
        settings.setEnabled(enabled);
        settings.setMaxPerCustomerPerDay(Math.max(0, maxPerCustomerPerDay));
        settings.setMaxPerTenantPerMonth(Math.max(0, maxPerTenantPerMonth));
        return settingsRepository.save(settings);
    }
}
