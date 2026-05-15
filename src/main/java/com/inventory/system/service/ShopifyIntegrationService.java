package com.inventory.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookStatus;
import com.inventory.system.common.entity.Category;
import com.inventory.system.common.entity.ProductImage;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ShopifyConnectionDto;
import com.inventory.system.payload.ShopifySyncResultDto;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.ProductTemplateRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.service.ingestion.ExternalOrderIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopifyIntegrationService {

    private static final String CATEGORY = "SHOPIFY";
    private static final String STORE_DOMAIN = "shopify.store_domain";
    private static final String CLIENT_ID = "shopify.client_id";
    private static final String CLIENT_SECRET = "shopify.client_secret";
    private static final String ADMIN_API_TOKEN = "shopify.admin_api_token";
    private static final String OAUTH_STATE = "shopify.oauth_state";
    private static final String OAUTH_SCOPES = "shopify.oauth_scopes";
    private static final String WEBHOOK_SECRET = "shopify.webhook_secret";
    private static final String ENABLED = "shopify.enabled";
    private static final String SYNC_CATALOG = "shopify.sync_catalog";
    private static final String SYNC_ORDERS = "shopify.sync_orders";
    private static final String SYNC_INVENTORY = "shopify.sync_inventory";
    private static final String HEALTH = "shopify.health";
    private static final String LAST_SYNC_AT = "shopify.last_sync_at";
    private static final String LAST_WEBHOOK_AT = "shopify.last_webhook_at";
    private static final String API_VERSION = "2026-04";
    private static final String REQUIRED_SCOPES = "read_products,read_orders";

    private final TenantSettingService tenantSettingService;
    private final CategoryRepository categoryRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ExternalOrderIngestionService externalOrderIngestionService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional(readOnly = true)
    public ShopifyConnectionDto getConnection(String publicBaseUrl) {
        return buildConnection(publicBaseUrl, false, false);
    }

    @Transactional
    public ShopifyConnectionDto saveConnection(ShopifyConnectionDto request, String publicBaseUrl) {
        if (request == null) {
            request = new ShopifyConnectionDto();
        }

        if (StringUtils.hasText(request.getStoreDomain())) {
            save(STORE_DOMAIN, normalizeDomain(request.getStoreDomain()), "STRING");
        }
        if (StringUtils.hasText(request.getClientId())) {
            save(CLIENT_ID, request.getClientId().trim(), "SECRET");
        }
        if (StringUtils.hasText(request.getClientSecret())) {
            save(CLIENT_SECRET, request.getClientSecret().trim(), "SECRET");
        }
        if (StringUtils.hasText(request.getAdminApiToken())) {
            save(ADMIN_API_TOKEN, request.getAdminApiToken().trim(), "SECRET");
        }
        if (StringUtils.hasText(request.getWebhookSecret())) {
            save(WEBHOOK_SECRET, request.getWebhookSecret().trim(), "SECRET");
        }
        save(ENABLED, String.valueOf(Boolean.TRUE.equals(request.getEnabled())), "BOOLEAN");
        save(SYNC_CATALOG, String.valueOf(!Boolean.FALSE.equals(request.getSyncCatalog())), "BOOLEAN");
        save(SYNC_ORDERS, String.valueOf(!Boolean.FALSE.equals(request.getSyncOrders())), "BOOLEAN");
        save(SYNC_INVENTORY, String.valueOf(!Boolean.FALSE.equals(request.getSyncInventory())), "BOOLEAN");

        ShopifyConnectionDto current = buildConnection(publicBaseUrl, false, false);
        save(HEALTH, isConfigured(current) ? "CONFIGURED" : hasOauthCredentials(current) ? "CONFIGURED" : "NOT_CONFIGURED", "STRING");
        return buildConnection(publicBaseUrl, false, false);
    }

    @Transactional
    public ShopifyConnectionDto startOAuthInstall(String publicBaseUrl) {
        ShopifyConnectionDto current = buildConnection(publicBaseUrl, false, true);
        if (!StringUtils.hasText(current.getStoreDomain())
                || !StringUtils.hasText(current.getClientId())
                || !StringUtils.hasText(current.getClientSecret())) {
            save(HEALTH, "MISSING_CREDENTIALS", "STRING");
            return buildConnection(publicBaseUrl, false, false);
        }
        String state = encodeState(TenantContext.getTenantId(), UUID.randomUUID().toString());
        save(OAUTH_STATE, state, "SECRET");
        save(OAUTH_SCOPES, REQUIRED_SCOPES, "STRING");
        return buildConnection(publicBaseUrl, false, false);
    }

    @Transactional
    public String completeOAuthInstall(Map<String, String[]> parameters, String publicBaseUrl) {
        ShopifyConnectionDto current = buildConnection(publicBaseUrl, false, true);
        String shop = firstParameter(parameters, "shop");
        String code = firstParameter(parameters, "code");
        String state = firstParameter(parameters, "state");
        String hmac = firstParameter(parameters, "hmac");

        if (!StringUtils.hasText(shop) || !StringUtils.hasText(code) || !StringUtils.hasText(state) || !StringUtils.hasText(hmac)) {
            save(HEALTH, "FAILED", "STRING");
            throw new IllegalArgumentException("Shopify callback is missing required OAuth parameters.");
        }
        if (!state.equals(setting(OAUTH_STATE, ""))) {
            save(HEALTH, "FAILED", "STRING");
            throw new IllegalArgumentException("Shopify OAuth state did not match the pending install.");
        }
        if (!verifyOAuthHmac(parameters, current.getClientSecret(), hmac)) {
            save(HEALTH, "FAILED", "STRING");
            throw new IllegalArgumentException("Shopify OAuth HMAC verification failed.");
        }

        String normalizedShop = normalizeDomain(shop);
        JsonNode tokenResponse = exchangeOAuthCode(normalizedShop, current.getClientId(), current.getClientSecret(), code);
        String accessToken = text(tokenResponse.get("access_token"));
        if (!StringUtils.hasText(accessToken)) {
            save(HEALTH, "FAILED", "STRING");
            throw new IllegalStateException("Shopify did not return an Admin API access token.");
        }

        save(STORE_DOMAIN, normalizedShop, "STRING");
        save(ADMIN_API_TOKEN, accessToken, "SECRET");
        save(OAUTH_SCOPES, firstText(tokenResponse.get("scope"), REQUIRED_SCOPES), "STRING");
        save(HEALTH, "CONNECTED", "STRING");
        return buildBackofficeReturnUrl(publicBaseUrl, "connected");
    }

    @Transactional
    public ShopifyConnectionDto testConnection(String publicBaseUrl) {
        ShopifyConnectionDto current = buildConnection(publicBaseUrl, true, true);
        if (!isConfigured(current)) {
            save(HEALTH, "MISSING_CREDENTIALS", "STRING");
            return buildConnection(publicBaseUrl, false, false);
        }
        try {
            JsonNode shop = shopifyGet(current, "/shop.json").path("shop");
            save(HEALTH, shop.path("id").isMissingNode() ? "FAILED" : "CONNECTED", "STRING");
        } catch (RuntimeException ex) {
            save(HEALTH, "FAILED", "STRING");
        }
        return buildConnection(publicBaseUrl, false, false);
    }

    @Transactional
    public void recordWebhookReceived() {
        save(LAST_WEBHOOK_AT, LocalDateTime.now().toString(), "STRING");
    }

    @Transactional
    public ShopifySyncResultDto syncProducts() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return ShopifySyncResultDto.builder()
                    .success(false)
                    .message("Shopify store domain and Admin API token are required before catalog sync.")
                    .build();
        }

        ShopifySyncResultDto result = ShopifySyncResultDto.builder()
                .success(true)
                .message("Shopify catalog sync completed.")
                .warnings(new ArrayList<>())
                .build();

        String endpoint = "/products.json?limit=250&status=any";
        int safetyCounter = 0;
        while (endpoint != null && safetyCounter++ < 25) {
            ShopifyPage page = shopifyGetPage(current, endpoint);
            for (JsonNode product : page.products()) {
                importProduct(product, result);
            }
            endpoint = page.nextEndpoint();
        }

        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        save(HEALTH, "CONNECTED", "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto syncOrders() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return ShopifySyncResultDto.builder()
                    .success(false)
                    .message("Shopify store domain and Admin API token are required before order sync.")
                    .build();
        }

        ShopifySyncResultDto result = ShopifySyncResultDto.builder()
                .success(true)
                .message("Shopify order sync completed.")
                .warnings(new ArrayList<>())
                .build();

        String endpoint = "/orders.json?status=any&limit=250";
        int safetyCounter = 0;
        while (endpoint != null && safetyCounter++ < 25) {
            ShopifyPage page = shopifyGetPage(current, endpoint, "orders");
            for (JsonNode order : page.items()) {
                result.setOrdersSeen(result.getOrdersSeen() + 1);
                try {
                    String payload = objectMapper.writeValueAsString(order);
                    ExternalOrderIngestionService.IngestionResult ingestionResult = externalOrderIngestionService.ingest(
                            ExternalOrderSource.SHOPIFY,
                            "orders/sync",
                            payload,
                            "manual-admin-sync");
                    if (ingestionResult.status() == InboundWebhookStatus.DUPLICATE) {
                        result.setOrdersDuplicate(result.getOrdersDuplicate() + 1);
                    } else if (ingestionResult.status() != InboundWebhookStatus.FAILED) {
                        result.setOrdersImported(result.getOrdersImported() + 1);
                    } else if (ingestionResult.error() != null) {
                        result.getWarnings().add(ingestionResult.error());
                    }
                } catch (Exception ex) {
                    result.getWarnings().add("Order import failed: " + ex.getMessage());
                }
            }
            endpoint = page.nextEndpoint();
        }

        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        save(HEALTH, "CONNECTED", "STRING");
        return result;
    }

    private void importProduct(JsonNode product, ShopifySyncResultDto result) {
        result.setProductsSeen(result.getProductsSeen() + 1);

        String shopifyProductId = text(product.get("id"));
        String title = firstText(product.get("title"), "Shopify product " + shopifyProductId);
        String handle = firstText(product.get("handle"), slugify(title));
        String description = stripHtml(text(product.get("body_html")));
        String categoryName = firstText(product.get("product_type"), text(product.get("vendor")), "Shopify");
        String status = firstText(product.get("status"), "active");

        Category category = resolveCategory(categoryName, result);
        Optional<ProductTemplate> existingTemplate = productTemplateRepository.findFirstByTenantIdAndStorefrontSlug(handle);
        ProductTemplate template = existingTemplate.orElseGet(ProductTemplate::new);
        if (existingTemplate.isPresent()) {
            result.setProductsUpdated(result.getProductsUpdated() + 1);
        } else {
            result.setProductsCreated(result.getProductsCreated() + 1);
        }
        template.setName(title);
        template.setDescription(description);
        template.setCategory(category);
        template.setIsActive(!"archived".equalsIgnoreCase(status));
        template.setPublishedToStorefront("active".equalsIgnoreCase(status));
        template.setStorefrontSlug(handle);
        template.setStorefrontTitle(title);
        template.setStorefrontDescription(description);
        template.setStorefrontSeoTitle(title);
        template.setStorefrontSeoDescription(description);
        template = productTemplateRepository.save(template);

        replaceImages(template, product.path("images"), result);
        importVariants(template, product.path("variants"), result, shopifyProductId);
    }

    private Category resolveCategory(String name, ShopifySyncResultDto result) {
        String categoryName = StringUtils.hasText(name) ? name.trim() : "Shopify";
        return categoryRepository.findFirstByTenantIdAndNameIgnoreCase(TenantContext.getTenantId(), categoryName)
                .orElseGet(() -> {
                    Category created = new Category();
                    created.setName(categoryName);
                    created.setDescription("Imported from Shopify.");
                    created.setPublishedToStorefront(true);
                    created.setStorefrontSlug(slugify(categoryName));
                    created.setStorefrontTitle(categoryName);
                    created.setStorefrontDescription("Imported from Shopify.");
                    result.setCategoriesCreated(result.getCategoriesCreated() + 1);
                    return categoryRepository.save(created);
                });
    }

    private void replaceImages(ProductTemplate template, JsonNode images, ShopifySyncResultDto result) {
        template.getImages().clear();
        if (!images.isArray()) {
            productTemplateRepository.save(template);
            return;
        }
        int index = 0;
        for (JsonNode image : images) {
            String src = text(image.get("src"));
            if (!StringUtils.hasText(src)) {
                continue;
            }
            ProductImage productImage = new ProductImage();
            productImage.setProductTemplate(template);
            productImage.setUrl(src);
            productImage.setFilename(filenameFromUrl(src, "shopify-image-" + index));
            productImage.setIsMain(index == 0);
            template.getImages().add(productImage);
            index++;
            result.setImagesImported(result.getImagesImported() + 1);
        }
        productTemplateRepository.save(template);
    }

    private void importVariants(ProductTemplate template, JsonNode variants, ShopifySyncResultDto result, String productId) {
        if (!variants.isArray() || variants.isEmpty()) {
            String sku = "SHOPIFY-" + productId;
            upsertVariant(template, sku, null, BigDecimal.ZERO, null, result);
            return;
        }
        for (JsonNode variant : variants) {
            String variantId = text(variant.get("id"));
            String sku = firstText(variant.get("sku"), "SHOPIFY-" + variantId);
            String barcode = text(variant.get("barcode"));
            BigDecimal price = decimal(variant.get("price"));
            BigDecimal compareAtPrice = decimalOrNull(variant.get("compare_at_price"));
            upsertVariant(template, sku, barcode, price, compareAtPrice, result);
        }
    }

    private void upsertVariant(ProductTemplate template, String sku, String barcode, BigDecimal price,
                               BigDecimal compareAtPrice, ShopifySyncResultDto result) {
        ProductVariant variant = productVariantRepository.findBySku(sku).orElseGet(ProductVariant::new);
        boolean existing = variant.getId() != null;
        variant.setTemplate(template);
        variant.setSku(sku);
        variant.setBarcode(StringUtils.hasText(barcode) ? barcode : null);
        variant.setPrice(price != null ? price : BigDecimal.ZERO);
        variant.setCompareAtPrice(compareAtPrice);
        variant.setStorefrontFeatured(false);
        productVariantRepository.save(variant);
        if (existing) {
            result.setVariantsUpdated(result.getVariantsUpdated() + 1);
        } else {
            result.setVariantsCreated(result.getVariantsCreated() + 1);
        }
    }

    private JsonNode shopifyGet(ShopifyConnectionDto connection, String endpoint) {
        return shopifyGetPage(connection, endpoint, "products").body();
    }

    private ShopifyPage shopifyGetPage(ShopifyConnectionDto connection, String endpoint) {
        return shopifyGetPage(connection, endpoint, "products");
    }

    private ShopifyPage shopifyGetPage(ShopifyConnectionDto connection, String endpoint, String arrayKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + connection.getStoreDomain() + "/admin/api/" + API_VERSION + endpoint))
                    .header("X-Shopify-Access-Token", connection.getAdminApiToken())
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Shopify API returned HTTP " + response.statusCode());
            }
            JsonNode body = objectMapper.readTree(response.body());
            return new ShopifyPage(body, body.path(arrayKey), nextEndpoint(response.headers().firstValue("link")));
        } catch (Exception ex) {
            throw new IllegalStateException("Shopify API request failed: " + ex.getMessage(), ex);
        }
    }

    private String nextEndpoint(Optional<String> linkHeader) {
        if (linkHeader.isEmpty()) {
            return null;
        }
        for (String part : linkHeader.get().split(",")) {
            if (!part.contains("rel=\"next\"")) {
                continue;
            }
            int start = part.indexOf('<');
            int end = part.indexOf('>');
            if (start >= 0 && end > start) {
                URI uri = URI.create(part.substring(start + 1, end));
                return uri.getPath().replace("/admin/api/" + API_VERSION, "") + "?" + uri.getQuery();
            }
        }
        return null;
    }

    private ShopifyConnectionDto buildConnection(String publicBaseUrl, boolean includeToken, boolean includeSecret) {
        String clientId = setting(CLIENT_ID, "");
        String clientSecret = setting(CLIENT_SECRET, "");
        String adminApiToken = setting(ADMIN_API_TOKEN, "");
        String webhookSecret = setting(WEBHOOK_SECRET, "");
        return ShopifyConnectionDto.builder()
                .storeDomain(setting(STORE_DOMAIN, ""))
            .clientId(includeSecret ? clientId : "")
            .clientSecret(includeSecret ? clientSecret : "")
                .adminApiToken(includeToken ? adminApiToken : "")
                .webhookSecret(includeSecret ? webhookSecret : "")
                .enabled(bool(ENABLED, false))
                .syncCatalog(bool(SYNC_CATALOG, true))
                .syncOrders(bool(SYNC_ORDERS, true))
                .syncInventory(bool(SYNC_INVENTORY, true))
                .health(setting(HEALTH, "NOT_CONFIGURED"))
                .lastSyncAt(setting(LAST_SYNC_AT, null))
                .lastWebhookAt(setting(LAST_WEBHOOK_AT, null))
                .clientIdConfigured(StringUtils.hasText(clientId))
                .clientSecretConfigured(StringUtils.hasText(clientSecret))
                .adminApiTokenConfigured(StringUtils.hasText(adminApiToken))
                .webhookSecretConfigured(StringUtils.hasText(webhookSecret))
                .webhookUrl(buildWebhookUrl(publicBaseUrl))
                .oauthCallbackUrl(buildOAuthCallbackUrl(publicBaseUrl))
                .installUrl(buildInstallUrl(publicBaseUrl, clientId))
                .oauthScopes(setting(OAUTH_SCOPES, REQUIRED_SCOPES))
                .build();
    }

    public static Optional<String> tenantFromOAuthState(String state) {
        if (!StringUtils.hasText(state)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            return separator > 0 ? Optional.of(decoded.substring(0, separator)) : Optional.empty();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String buildWebhookUrl(String publicBaseUrl) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/api/webhooks/shopify/" + TenantContext.getTenantId() + "/orders";
        }
        return publicBaseUrl.replaceAll("/+$", "") + "/api/webhooks/shopify/" + TenantContext.getTenantId() + "/orders";
    }

    private String buildOAuthCallbackUrl(String publicBaseUrl) {
        String path = "/api/v1/integrations/shopify/oauth/callback";
        if (!StringUtils.hasText(publicBaseUrl)) {
            return path;
        }
        return publicBaseUrl.replaceAll("/+$", "") + path;
    }

    private String buildInstallUrl(String publicBaseUrl, String clientId) {
        String storeDomain = setting(STORE_DOMAIN, "");
        String state = setting(OAUTH_STATE, "");
        if (!StringUtils.hasText(storeDomain) || !StringUtils.hasText(clientId) || !StringUtils.hasText(publicBaseUrl) || !StringUtils.hasText(state)) {
            return null;
        }
        return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(storeDomain)
                .path("/admin/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("scope", REQUIRED_SCOPES)
                .queryParam("redirect_uri", buildOAuthCallbackUrl(publicBaseUrl))
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    private String buildBackofficeReturnUrl(String publicBaseUrl, String status) {
        String base = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl.replaceAll("/+$", "") : "";
        return base + "/inventory/plugins/shopify?shopify=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
    }

    private String encodeState(String tenantId, String nonce) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((tenantId + ":" + nonce).getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasOauthCredentials(ShopifyConnectionDto dto) {
        return StringUtils.hasText(dto.getStoreDomain())
                && StringUtils.hasText(dto.getClientId())
                && StringUtils.hasText(dto.getClientSecret());
    }

    private boolean isConfigured(ShopifyConnectionDto dto) {
        return StringUtils.hasText(dto.getStoreDomain()) && StringUtils.hasText(dto.getAdminApiToken());
    }

    private JsonNode exchangeOAuthCode(String shop, String clientId, String clientSecret, String code) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "code", code));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + shop + "/admin/oauth/access_token"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Shopify token exchange returned HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (Exception ex) {
            throw new IllegalStateException("Shopify token exchange failed: " + ex.getMessage(), ex);
        }
    }

    private boolean verifyOAuthHmac(Map<String, String[]> parameters, String clientSecret, String receivedHmac) {
        if (!StringUtils.hasText(clientSecret)) {
            return false;
        }
        try {
            String message = parameters.entrySet().stream()
                    .filter(entry -> !"hmac".equals(entry.getKey()) && !"signature".equals(entry.getKey()))
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> entry.getKey() + "=" + String.join(",", List.of(entry.getValue())))
                    .collect(Collectors.joining("&"));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String calculated = bytesToHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            return calculated.equalsIgnoreCase(receivedHmac);
        } catch (Exception ex) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String firstParameter(Map<String, String[]> parameters, String key) {
        String[] values = parameters.get(key);
        return values == null || values.length == 0 ? null : values[0];
    }

    private void save(String key, String value, String type) {
        tenantSettingService.updateSetting(key, value, type, CATEGORY);
    }

    private String setting(String key, String fallback) {
        return tenantSettingService.findSetting(key).map(s -> s.getValue()).orElse(fallback);
    }

    private boolean bool(String key, boolean fallback) {
        return Boolean.parseBoolean(setting(key, String.valueOf(fallback)));
    }

    private String normalizeDomain(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("https://", "")
                .replace("http://", "");
        int slash = normalized.indexOf('/');
        return slash >= 0 ? normalized.substring(0, slash) : normalized;
    }

    private String slugify(String value) {
        String slug = value == null ? "shopify" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.hasText(slug) ? slug : "shopify";
    }

    private String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String filenameFromUrl(String url, String fallback) {
        try {
            String path = URI.create(url).getPath();
            int slash = path.lastIndexOf('/');
            String filename = slash >= 0 ? path.substring(slash + 1) : path;
            return StringUtils.hasText(filename) ? filename : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstText(JsonNode first, String fallback) {
        String value = text(first);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstText(JsonNode first, String second, String fallback) {
        String value = text(first);
        if (StringUtils.hasText(value)) return value;
        return StringUtils.hasText(second) ? second : fallback;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asText();
    }

    private BigDecimal decimal(JsonNode node) {
        BigDecimal value = decimalOrNull(node);
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        String value = text(node);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record ShopifyPage(JsonNode body, JsonNode items, String nextEndpoint) {
        JsonNode products() {
            return items;
        }
    }
}