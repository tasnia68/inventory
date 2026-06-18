package com.inventory.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.Category;
import com.inventory.system.common.entity.ExternalOrderSource;
import com.inventory.system.common.entity.InboundWebhookStatus;
import com.inventory.system.common.entity.ProductAttribute;
import com.inventory.system.common.entity.ProductAttributeValue;
import com.inventory.system.common.entity.ProductImage;
import com.inventory.system.common.entity.ProductTemplate;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.StockMovement.StockMovementType;
import com.inventory.system.common.entity.StockStatus;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ShopifyConnectionDto;
import com.inventory.system.payload.ShopifySyncResultDto;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.ProductAttributeRepository;
import com.inventory.system.repository.ProductAttributeValueRepository;
import com.inventory.system.repository.ProductTemplateRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.StockRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.WarehouseRepository;
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
import java.util.HashMap;
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

    private static final String MAP_LOCATION_PREFIX = "shopify.map.location.";
    private static final String MAP_PRODUCT_PREFIX = "shopify.map.product.";
    private static final String MAP_VARIANT_PREFIX = "shopify.map.variant.";
    private static final String MAP_INVENTORY_ITEM_PREFIX = "shopify.map.inventoryitem.";

    private static final String API_VERSION = "2024-10";
    private static final String REQUIRED_SCOPES = "read_products,write_products,read_orders,read_inventory,write_inventory,read_locations,read_customers";

    private final TenantSettingService tenantSettingService;
    private final TenantSettingRepository tenantSettingRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTemplateRepository productTemplateRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final ExternalOrderIngestionService externalOrderIngestionService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final StockService stockService;

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
            JsonNode data = shopifyGraphQL(current, "{ shop { id name myshopifyDomain } }", null);
            save(HEALTH, data.path("shop").path("id").isMissingNode() ? "FAILED" : "CONNECTED", "STRING");
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
            return failResult("Shopify store domain and Admin API token are required before catalog sync.");
        }
        ShopifySyncResultDto result = okResult("Shopify catalog sync completed.");

        String cursor = null;
        int pageGuard = 0;
        while (pageGuard++ < 100) {
            Map<String, Object> vars = cursor == null ? Map.of() : Map.of("after", cursor);
            JsonNode data = shopifyGraphQL(current, PRODUCTS_QUERY, vars);
            JsonNode products = data.path("products");
            for (JsonNode edge : products.path("edges")) {
                importProductGql(edge.path("node"), result);
            }
            if (!products.path("pageInfo").path("hasNextPage").asBoolean(false)) {
                break;
            }
            cursor = products.path("pageInfo").path("endCursor").asText(null);
            if (cursor == null) {
                break;
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        save(HEALTH, "CONNECTED", "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto syncOrders() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return failResult("Shopify store domain and Admin API token are required before order sync.");
        }
        ShopifySyncResultDto result = okResult("Shopify order sync completed.");

        String cursor = null;
        int pageGuard = 0;
        while (pageGuard++ < 100) {
            Map<String, Object> vars = cursor == null ? Map.of() : Map.of("after", cursor);
            JsonNode data = shopifyGraphQL(current, ORDERS_QUERY, vars);
            JsonNode orders = data.path("orders");
            for (JsonNode edge : orders.path("edges")) {
                JsonNode order = edge.path("node");
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
            if (!orders.path("pageInfo").path("hasNextPage").asBoolean(false)) {
                break;
            }
            cursor = orders.path("pageInfo").path("endCursor").asText(null);
            if (cursor == null) {
                break;
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        save(HEALTH, "CONNECTED", "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto syncLocations() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return failResult("Shopify store domain and Admin API token are required before location sync.");
        }
        ShopifySyncResultDto result = okResult("Shopify location sync completed.");

        JsonNode data = shopifyGraphQL(current, LOCATIONS_QUERY, null);
        for (JsonNode edge : data.path("locations").path("edges")) {
            JsonNode loc = edge.path("node");
            result.setLocationsSeen(result.getLocationsSeen() + 1);
            String gid = text(loc.get("id"));
            String name = firstText(loc.get("name"), "Shopify Location");
            String address1 = text(loc.path("address").get("address1"));
            String city = text(loc.path("address").get("city"));
            String country = text(loc.path("address").get("country"));
            String addressLine = joinNonBlank(", ", address1, city, country);

            Warehouse existing = lookupWarehouseForShopifyLocation(gid)
                    .or(() -> warehouseRepository.findByName(name))
                    .orElse(null);
            if (existing == null) {
                Warehouse wh = new Warehouse();
                wh.setName(name);
                wh.setType("STORE");
                wh.setIsActive(loc.path("isActive").asBoolean(true));
                wh.setLocation(StringUtils.hasText(addressLine) ? addressLine : "Imported from Shopify");
                Warehouse saved = warehouseRepository.save(wh);
                save(MAP_LOCATION_PREFIX + gid, saved.getId().toString(), "STRING");
                result.setLocationsCreated(result.getLocationsCreated() + 1);
            } else {
                if (StringUtils.hasText(addressLine) && !addressLine.equals(existing.getLocation())) {
                    existing.setLocation(addressLine);
                    warehouseRepository.save(existing);
                }
                save(MAP_LOCATION_PREFIX + gid, existing.getId().toString(), "STRING");
                result.setLocationsMatched(result.getLocationsMatched() + 1);
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto syncInventoryLevels() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return failResult("Shopify store domain and Admin API token are required before inventory sync.");
        }
        ShopifySyncResultDto result = okResult("Shopify inventory sync completed.");

        Map<String, UUID> locationMap = locationMap();
        if (locationMap.isEmpty()) {
            result.getWarnings().add("No location mapping found. Run \"Sync locations\" first to map Shopify locations to warehouses.");
        }

        String cursor = null;
        int pageGuard = 0;
        while (pageGuard++ < 100) {
            Map<String, Object> vars = cursor == null ? Map.of() : Map.of("after", cursor);
            JsonNode data = shopifyGraphQL(current, INVENTORY_QUERY, vars);
            JsonNode variants = data.path("productVariants");
            for (JsonNode edge : variants.path("edges")) {
                JsonNode variant = edge.path("node");
                String sku = text(variant.get("sku"));
                if (!StringUtils.hasText(sku)) {
                    continue;
                }
                Optional<ProductVariant> localVariant = productVariantRepository.findBySku(sku);
                if (localVariant.isEmpty()) {
                    continue;
                }
                String inventoryItemGid = text(variant.path("inventoryItem").get("id"));
                if (StringUtils.hasText(inventoryItemGid)) {
                    save(MAP_INVENTORY_ITEM_PREFIX + localVariant.get().getId(), inventoryItemGid, "STRING");
                }

                for (JsonNode lvlEdge : variant.path("inventoryItem").path("inventoryLevels").path("edges")) {
                    JsonNode level = lvlEdge.path("node");
                    String locationGid = text(level.path("location").get("id"));
                    UUID warehouseId = locationMap.get(locationGid);
                    if (warehouseId == null) {
                        continue;
                    }
                    BigDecimal onHand = BigDecimal.ZERO;
                    for (JsonNode q : level.path("quantities")) {
                        if ("on_hand".equals(text(q.get("name")))) {
                            onHand = decimal(q.get("quantity"));
                        }
                    }
                    result.setStockLevelsSeen(result.getStockLevelsSeen() + 1);
                    try {
                        applyStockLevel(localVariant.get(), warehouseId, onHand);
                        result.setStockLevelsApplied(result.getStockLevelsApplied() + 1);
                    } catch (RuntimeException ex) {
                        result.getWarnings().add("Inventory apply failed for " + sku + ": " + ex.getMessage());
                    }
                }
            }
            if (!variants.path("pageInfo").path("hasNextPage").asBoolean(false)) {
                break;
            }
            cursor = variants.path("pageInfo").path("endCursor").asText(null);
            if (cursor == null) {
                break;
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto pushCatalog() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return failResult("Shopify store domain and Admin API token are required before catalog push.");
        }
        ShopifySyncResultDto result = okResult("Shopify catalog push completed.");

        List<ProductTemplate> templates = productTemplateRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getPublishedToStorefront()))
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> setting(MAP_PRODUCT_PREFIX + t.getId(), null) == null)
                .toList();

        for (ProductTemplate template : templates) {
            try {
                JsonNode created = pushProductMutation(current, template);
                String gid = text(created.path("product").get("id"));
                if (!StringUtils.hasText(gid)) {
                    String userErrs = created.path("userErrors").toString();
                    result.getWarnings().add("productCreate failed for " + template.getName() + ": " + userErrs);
                    continue;
                }
                save(MAP_PRODUCT_PREFIX + template.getId(), gid, "STRING");
                result.setProductsPushed(result.getProductsPushed() + 1);

                List<ProductVariant> variants = productVariantRepository.findByTemplateId(template.getId());
                if (!variants.isEmpty()) {
                    JsonNode varRes = pushVariantsMutation(current, gid, variants);
                    JsonNode createdVariants = varRes.path("productVariants");
                    for (JsonNode v : createdVariants) {
                        String vGid = text(v.get("id"));
                        String vSku = text(v.get("sku"));
                        productVariantRepository.findBySku(vSku).ifPresent(local ->
                                save(MAP_VARIANT_PREFIX + local.getId(), vGid, "STRING"));
                        result.setVariantsPushed(result.getVariantsPushed() + 1);
                    }
                    JsonNode varErrs = varRes.path("userErrors");
                    if (varErrs.isArray() && !varErrs.isEmpty()) {
                        result.getWarnings().add("productVariantsBulkCreate warnings for " + template.getName() + ": " + varErrs);
                    }
                }
            } catch (RuntimeException ex) {
                result.getWarnings().add("Push failed for " + template.getName() + ": " + ex.getMessage());
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        return result;
    }

    @Transactional
    public ShopifySyncResultDto pushInventory() {
        ShopifyConnectionDto current = buildConnection(null, true, true);
        if (!isConfigured(current)) {
            return failResult("Shopify store domain and Admin API token are required before inventory push.");
        }
        ShopifySyncResultDto result = okResult("Shopify inventory push completed.");

        Map<String, UUID> locationMap = locationMap();
        if (locationMap.isEmpty()) {
            result.getWarnings().add("No location mapping found. Run \"Sync locations\" first.");
            return result;
        }
        Map<UUID, String> warehouseToLocation = new HashMap<>();
        locationMap.forEach((gid, whId) -> warehouseToLocation.put(whId, gid));

        List<TenantSetting> inventoryItemMappings = tenantSettingRepository.findByCategory(CATEGORY).stream()
                .filter(s -> s.getSettingKey() != null && s.getSettingKey().startsWith(MAP_INVENTORY_ITEM_PREFIX))
                .toList();

        for (TenantSetting mapping : inventoryItemMappings) {
            String variantIdRaw = mapping.getSettingKey().substring(MAP_INVENTORY_ITEM_PREFIX.length());
            UUID variantId;
            try {
                variantId = UUID.fromString(variantIdRaw);
            } catch (IllegalArgumentException invalid) {
                continue;
            }
            String inventoryItemGid = mapping.getSettingValue();
            if (!StringUtils.hasText(inventoryItemGid)) {
                continue;
            }
            for (Map.Entry<UUID, String> wh : warehouseToLocation.entrySet()) {
                BigDecimal onHand = Optional.ofNullable(
                        productVariantRepository.findById(variantId).map(v ->
                                Optional.ofNullable(
                                        productVariantQuantitySum(variantId, wh.getKey())
                                ).orElse(BigDecimal.ZERO)
                        ).orElse(BigDecimal.ZERO)
                ).orElse(BigDecimal.ZERO);

                try {
                    JsonNode response = pushInventoryMutation(current, inventoryItemGid, wh.getValue(), onHand.intValue());
                    JsonNode errs = response.path("userErrors");
                    if (errs.isArray() && !errs.isEmpty()) {
                        result.getWarnings().add("inventorySetOnHandQuantities for variant " + variantId + ": " + errs);
                    } else {
                        result.setInventoryAdjustmentsPushed(result.getInventoryAdjustmentsPushed() + 1);
                    }
                } catch (RuntimeException ex) {
                    result.getWarnings().add("Inventory push failed for variant " + variantId + ": " + ex.getMessage());
                }
            }
        }
        save(LAST_SYNC_AT, LocalDateTime.now().toString(), "STRING");
        return result;
    }

    private BigDecimal productVariantQuantitySum(UUID variantId, UUID warehouseId) {
        BigDecimal total = stockRepository.countTotalQuantityByProductVariantAndWarehouse(variantId, warehouseId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void importProductGql(JsonNode product, ShopifySyncResultDto result) {
        result.setProductsSeen(result.getProductsSeen() + 1);

        String gid = text(product.get("id"));
        String title = firstText(product.get("title"), "Shopify product " + numericTail(gid));
        String handle = firstText(product.get("handle"), slugify(title));
        String descriptionHtml = text(product.get("descriptionHtml"));
        String description = stripHtml(descriptionHtml);
        String vendor = text(product.get("vendor"));
        String productType = text(product.get("productType"));
        String status = firstText(product.get("status"), "ACTIVE");

        StringBuilder tagBuilder = new StringBuilder();
        for (JsonNode tag : product.path("tags")) {
            if (tagBuilder.length() > 0) tagBuilder.append(", ");
            tagBuilder.append(tag.asText());
        }
        String tagCsv = tagBuilder.length() == 0 ? null : tagBuilder.toString();

        String categoryName = firstText(product.get("productType"), vendor, "Shopify");
        Category category = resolveCategory(categoryName, result);

        Optional<ProductTemplate> existing = productTemplateRepository.findFirstByTenantIdAndStorefrontSlug(handle);
        ProductTemplate template = existing.orElseGet(ProductTemplate::new);
        if (existing.isPresent()) {
            result.setProductsUpdated(result.getProductsUpdated() + 1);
        } else {
            result.setProductsCreated(result.getProductsCreated() + 1);
        }

        template.setName(title);
        template.setDescription(description);
        template.setCategory(category);
        template.setIsActive(!"ARCHIVED".equalsIgnoreCase(status));
        template.setPublishedToStorefront("ACTIVE".equalsIgnoreCase(status));
        template.setStorefrontSlug(handle);
        template.setStorefrontTitle(title);
        template.setStorefrontDescription(description);
        template.setStorefrontSeoTitle(title);
        template.setStorefrontSeoDescription(description);
        template.setStatus(status.toUpperCase(Locale.ROOT));
        template.setVendor(vendor);
        template.setProductType(productType);
        template.setTags(tagCsv);
        template = productTemplateRepository.save(template);
        save(MAP_PRODUCT_PREFIX + template.getId(), gid, "STRING");

        replaceImagesGql(template, product.path("images"), result);
        Map<String, ProductAttribute> attributeMap = ensureAttributes(template, product.path("options"));
        importVariantsGql(template, product.path("variants"), result, gid, attributeMap);
    }

    /**
     * Mirror Shopify product options (Color, Size, ...) onto template-scoped
     * ProductAttributes. Shopify's single-variant placeholder option ("Title" with the
     * single value "Default Title") is skipped. Returns a name-keyed map so per-variant
     * selectedOptions can be resolved back to the right attribute.
     */
    private Map<String, ProductAttribute> ensureAttributes(ProductTemplate template, JsonNode optionsNode) {
        Map<String, ProductAttribute> byName = new HashMap<>();
        if (!optionsNode.isArray()) {
            return byName;
        }
        List<ProductAttribute> existing = productAttributeRepository.findByTemplateId(template.getId());
        Map<String, ProductAttribute> existingByName = new HashMap<>();
        for (ProductAttribute attr : existing) {
            if (attr.getName() != null) {
                existingByName.put(attr.getName().toLowerCase(Locale.ROOT), attr);
            }
        }
        for (JsonNode option : optionsNode) {
            String name = text(option.get("name"));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if ("Title".equalsIgnoreCase(name) && isDefaultTitleOnly(option.path("values"))) {
                continue;
            }
            ProductAttribute attribute = existingByName.get(name.toLowerCase(Locale.ROOT));
            if (attribute == null) {
                attribute = new ProductAttribute();
                attribute.setName(name);
                attribute.setType(ProductAttribute.AttributeType.DROPDOWN);
                attribute.setRequired(false);
                attribute.setTemplate(template);
                StringBuilder opts = new StringBuilder();
                for (JsonNode val : option.path("values")) {
                    if (opts.length() > 0) opts.append(",");
                    opts.append(val.asText());
                }
                attribute.setOptions(opts.toString());
                attribute = productAttributeRepository.save(attribute);
            }
            byName.put(name.toLowerCase(Locale.ROOT), attribute);
        }
        return byName;
    }

    private boolean isDefaultTitleOnly(JsonNode values) {
        if (!values.isArray() || values.size() != 1) {
            return false;
        }
        return "Default Title".equalsIgnoreCase(values.get(0).asText());
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

    private void replaceImagesGql(ProductTemplate template, JsonNode imagesConn, ShopifySyncResultDto result) {
        template.getImages().clear();
        if (!imagesConn.isObject()) {
            productTemplateRepository.save(template);
            return;
        }
        int index = 0;
        for (JsonNode edge : imagesConn.path("edges")) {
            JsonNode image = edge.path("node");
            String src = text(image.get("url"));
            if (!StringUtils.hasText(src)) {
                continue;
            }
            String filename = filenameFromUrl(src, "shopify-image-" + index + ".jpg");
            // The whole stack (storefront, admin, /file streaming) treats ProductImage.url
            // as a MinIO object key, NOT an external URL. So we download the Shopify CDN
            // binary into MinIO and store the resulting key — otherwise every imported
            // image 404s on display. Failure of one image must not abort the product.
            String objectKey = downloadToStorage(src, filename, template.getId());
            if (objectKey == null) {
                result.getWarnings().add("Image download failed for " + template.getName() + ": " + src);
                continue;
            }
            ProductImage productImage = new ProductImage();
            productImage.setProductTemplate(template);
            productImage.setUrl(objectKey);
            productImage.setFilename(filename);
            productImage.setIsMain(index == 0);
            template.getImages().add(productImage);
            index++;
            result.setImagesImported(result.getImagesImported() + 1);
        }
        productTemplateRepository.save(template);
    }

    private String downloadToStorage(String url, String filename, UUID templateId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "image/*")
                    .timeout(java.time.Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return null;
            }
            String contentType = response.headers().firstValue("content-type").orElse(guessContentType(filename));
            return fileStorageService.uploadBytes(body, filename, contentType, "product-images/" + templateId);
        } catch (Exception ex) {
            return null;
        }
    }

    private String guessContentType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }

    private void importVariantsGql(ProductTemplate template, JsonNode variantsConn, ShopifySyncResultDto result,
                                   String productGid, Map<String, ProductAttribute> attributeMap) {
        JsonNode edges = variantsConn.path("edges");
        if (!edges.isArray() || edges.isEmpty()) {
            String sku = "SHOPIFY-" + numericTail(productGid);
            ProductVariant created = upsertVariant(template, sku, null, BigDecimal.ZERO, null, null, result);
            save(MAP_VARIANT_PREFIX + created.getId(), productGid, "STRING");
            return;
        }
        for (JsonNode edge : edges) {
            JsonNode variant = edge.path("node");
            String variantGid = text(variant.get("id"));
            String sku = firstText(variant.get("sku"), "SHOPIFY-" + numericTail(variantGid));
            String barcode = text(variant.get("barcode"));
            BigDecimal price = decimal(variant.get("price"));
            BigDecimal compareAtPrice = decimalOrNull(variant.get("compareAtPrice"));
            BigDecimal cost = decimalOrNull(variant.path("inventoryItem").path("unitCost").get("amount"));
            ProductVariant saved = upsertVariant(template, sku, barcode, price, compareAtPrice, cost, result);
            if (StringUtils.hasText(variantGid)) {
                save(MAP_VARIANT_PREFIX + saved.getId(), variantGid, "STRING");
            }
            String inventoryItemGid = text(variant.path("inventoryItem").get("id"));
            if (StringUtils.hasText(inventoryItemGid)) {
                save(MAP_INVENTORY_ITEM_PREFIX + saved.getId(), inventoryItemGid, "STRING");
            }
            applyVariantAttributes(saved, variant.path("selectedOptions"), attributeMap);
        }
    }

    private ProductVariant upsertVariant(ProductTemplate template, String sku, String barcode, BigDecimal price,
                                         BigDecimal compareAtPrice, BigDecimal cost, ShopifySyncResultDto result) {
        ProductVariant variant = productVariantRepository.findBySku(sku).orElseGet(ProductVariant::new);
        boolean existing = variant.getId() != null;
        variant.setTemplate(template);
        variant.setSku(sku);
        variant.setBarcode(StringUtils.hasText(barcode) ? barcode : null);
        variant.setPrice(price != null ? price : BigDecimal.ZERO);
        variant.setCompareAtPrice(compareAtPrice);
        if (cost != null) {
            variant.setCost(cost);
        }
        if (variant.getStorefrontFeatured() == null) {
            variant.setStorefrontFeatured(false);
        }
        ProductVariant saved = productVariantRepository.save(variant);
        if (existing) {
            result.setVariantsUpdated(result.getVariantsUpdated() + 1);
        } else {
            result.setVariantsCreated(result.getVariantsCreated() + 1);
        }
        return saved;
    }

    /**
     * Translate Shopify per-variant selectedOptions (Color: Red, Size: M) into
     * ProductAttributeValue rows so the variant is distinguishable in the catalog UI and
     * storefront. Re-import is idempotent: existing values for the variant are replaced.
     */
    private void applyVariantAttributes(ProductVariant variant, JsonNode selectedOptions,
                                        Map<String, ProductAttribute> attributeMap) {
        if (attributeMap.isEmpty() || !selectedOptions.isArray()) {
            return;
        }
        List<ProductAttributeValue> existing = productAttributeValueRepository.findByVariantId(variant.getId());
        if (!existing.isEmpty()) {
            productAttributeValueRepository.deleteAll(existing);
        }
        List<ProductAttributeValue> toSave = new ArrayList<>();
        for (JsonNode option : selectedOptions) {
            String name = text(option.get("name"));
            String value = text(option.get("value"));
            if (!StringUtils.hasText(name) || !StringUtils.hasText(value)) {
                continue;
            }
            if ("Title".equalsIgnoreCase(name) && "Default Title".equalsIgnoreCase(value)) {
                continue;
            }
            ProductAttribute attribute = attributeMap.get(name.toLowerCase(Locale.ROOT));
            if (attribute == null) {
                continue;
            }
            ProductAttributeValue pav = new ProductAttributeValue();
            pav.setVariant(variant);
            pav.setAttribute(attribute);
            pav.setValue(value);
            toSave.add(pav);
        }
        if (!toSave.isEmpty()) {
            productAttributeValueRepository.saveAll(toSave);
        }
    }

    private void applyStockLevel(ProductVariant variant, UUID warehouseId, BigDecimal targetOnHand) {
        BigDecimal current = productVariantQuantitySum(variant.getId(), warehouseId);
        BigDecimal delta = targetOnHand.subtract(current);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setProductVariantId(variant.getId());
        dto.setWarehouseId(warehouseId);
        dto.setQuantity(delta);
        dto.setStockStatus(StockStatus.AVAILABLE);
        dto.setType(delta.compareTo(BigDecimal.ZERO) > 0 ? StockMovementType.IN : StockMovementType.OUT);
        dto.setReason("Shopify inventory sync (target=" + targetOnHand + ")");
        dto.setReferenceId("shopify:" + LocalDateTime.now());
        stockService.adjustStock(dto);
    }

    private Optional<Warehouse> lookupWarehouseForShopifyLocation(String gid) {
        String value = setting(MAP_LOCATION_PREFIX + gid, null);
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return warehouseRepository.findById(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Map<String, UUID> locationMap() {
        Map<String, UUID> map = new HashMap<>();
        for (TenantSetting setting : tenantSettingRepository.findByCategory(CATEGORY)) {
            if (setting.getSettingKey() == null || !setting.getSettingKey().startsWith(MAP_LOCATION_PREFIX)) {
                continue;
            }
            String gid = setting.getSettingKey().substring(MAP_LOCATION_PREFIX.length());
            try {
                map.put(gid, UUID.fromString(setting.getSettingValue()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return map;
    }

    private JsonNode pushProductMutation(ShopifyConnectionDto connection, ProductTemplate template) {
        Map<String, Object> input = new HashMap<>();
        input.put("title", template.getName());
        if (StringUtils.hasText(template.getStorefrontDescription())) {
            input.put("descriptionHtml", template.getStorefrontDescription());
        }
        if (StringUtils.hasText(template.getStorefrontSlug())) {
            input.put("handle", template.getStorefrontSlug());
        }
        if (StringUtils.hasText(template.getVendor())) {
            input.put("vendor", template.getVendor());
        }
        if (StringUtils.hasText(template.getProductType())) {
            input.put("productType", template.getProductType());
        }
        if (StringUtils.hasText(template.getTags())) {
            input.put("tags", splitTags(template.getTags()));
        }
        input.put("status", Boolean.TRUE.equals(template.getPublishedToStorefront()) ? "ACTIVE" : "DRAFT");

        JsonNode data = shopifyGraphQL(connection, PRODUCT_CREATE_MUTATION, Map.of("input", input));
        return data.path("productCreate");
    }

    private JsonNode pushVariantsMutation(ShopifyConnectionDto connection, String productGid, List<ProductVariant> variants) {
        List<Map<String, Object>> inputs = new ArrayList<>();
        for (ProductVariant v : variants) {
            Map<String, Object> i = new HashMap<>();
            i.put("price", v.getPrice() == null ? "0" : v.getPrice().toPlainString());
            if (v.getCompareAtPrice() != null) {
                i.put("compareAtPrice", v.getCompareAtPrice().toPlainString());
            }
            Map<String, Object> inventoryItem = new HashMap<>();
            inventoryItem.put("sku", v.getSku());
            inventoryItem.put("tracked", true);
            if (v.getCost() != null) {
                inventoryItem.put("cost", v.getCost().toPlainString());
            }
            i.put("inventoryItem", inventoryItem);
            if (StringUtils.hasText(v.getBarcode())) {
                i.put("barcode", v.getBarcode());
            }
            inputs.add(i);
        }
        JsonNode data = shopifyGraphQL(connection, PRODUCT_VARIANTS_BULK_CREATE_MUTATION,
                Map.of("productId", productGid, "variants", inputs));
        return data.path("productVariantsBulkCreate");
    }

    private JsonNode pushInventoryMutation(ShopifyConnectionDto connection, String inventoryItemGid,
                                           String locationGid, int onHand) {
        Map<String, Object> setQty = new HashMap<>();
        setQty.put("inventoryItemId", inventoryItemGid);
        setQty.put("locationId", locationGid);
        setQty.put("quantity", onHand);

        Map<String, Object> input = new HashMap<>();
        input.put("reason", "correction");
        input.put("referenceDocumentUri", "logistra://inventory-sync");
        input.put("setQuantities", List.of(setQty));

        JsonNode data = shopifyGraphQL(connection, INVENTORY_SET_ON_HAND_MUTATION, Map.of("input", input));
        return data.path("inventorySetOnHandQuantities");
    }

    private List<String> splitTags(String csv) {
        List<String> out = new ArrayList<>();
        for (String p : csv.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private JsonNode shopifyGraphQL(ShopifyConnectionDto connection, String query, Map<String, Object> variables) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("variables", variables == null ? Map.of() : variables);
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + connection.getStoreDomain() + "/admin/api/" + API_VERSION + "/graphql.json"))
                    .header("X-Shopify-Access-Token", connection.getAdminApiToken())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Shopify GraphQL HTTP " + response.statusCode() + ": " + truncate(response.body(), 400));
            }
            JsonNode parsed = objectMapper.readTree(response.body());
            JsonNode errors = parsed.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                throw new IllegalStateException("Shopify GraphQL errors: " + errors.toString());
            }
            return parsed.path("data");
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException) {
                throw (IllegalStateException) ex;
            }
            throw new IllegalStateException("Shopify GraphQL request failed: " + ex.getMessage(), ex);
        }
    }

    private static final String PRODUCTS_QUERY = "query ProductPage($after: String) {\n" +
            "  products(first: 50, after: $after) {\n" +
            "    pageInfo { hasNextPage endCursor }\n" +
            "    edges {\n" +
            "      node {\n" +
            "        id title handle status vendor productType tags\n" +
            "        descriptionHtml createdAt updatedAt\n" +
            "        featuredImage { url altText }\n" +
            "        images(first: 20) { edges { node { id url altText } } }\n" +
            "        options { id name values }\n" +
            "        variants(first: 100) {\n" +
            "          edges {\n" +
            "            node {\n" +
            "              id title sku barcode price compareAtPrice\n" +
            "              selectedOptions { name value }\n" +
            "              inventoryItem { id sku unitCost { amount } }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String ORDERS_QUERY = "query OrderPage($after: String) {\n" +
            "  orders(first: 50, after: $after) {\n" +
            "    pageInfo { hasNextPage endCursor }\n" +
            "    edges {\n" +
            "      node {\n" +
            "        id name createdAt processedAt cancelledAt\n" +
            "        displayFinancialStatus displayFulfillmentStatus\n" +
            "        email phone\n" +
            "        customer { id email firstName lastName phone }\n" +
            "        shippingAddress { address1 address2 city province country zip name phone }\n" +
            "        billingAddress { address1 address2 city province country zip name phone }\n" +
            "        currentTotalPriceSet { presentmentMoney { amount currencyCode } }\n" +
            "        totalPriceSet { presentmentMoney { amount currencyCode } }\n" +
            "        subtotalPriceSet { presentmentMoney { amount currencyCode } }\n" +
            "        totalTaxSet { presentmentMoney { amount currencyCode } }\n" +
            "        lineItems(first: 50) {\n" +
            "          edges {\n" +
            "            node {\n" +
            "              id title sku quantity\n" +
            "              originalUnitPriceSet { shopMoney { amount currencyCode } }\n" +
            "              variant { id sku }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String LOCATIONS_QUERY = "{\n" +
            "  locations(first: 100, includeInactive: true) {\n" +
            "    edges {\n" +
            "      node {\n" +
            "        id name isActive\n" +
            "        address { address1 address2 city province country zip }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String INVENTORY_QUERY = "query InventoryPage($after: String) {\n" +
            "  productVariants(first: 100, after: $after) {\n" +
            "    pageInfo { hasNextPage endCursor }\n" +
            "    edges {\n" +
            "      node {\n" +
            "        id sku\n" +
            "        inventoryItem {\n" +
            "          id\n" +
            "          inventoryLevels(first: 20) {\n" +
            "            edges {\n" +
            "              node {\n" +
            "                id\n" +
            "                location { id name }\n" +
            "                quantities(names: [\"available\",\"on_hand\"]) { name quantity }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String PRODUCT_CREATE_MUTATION = "mutation productCreate($input: ProductInput!) {\n" +
            "  productCreate(input: $input) {\n" +
            "    product { id title handle status }\n" +
            "    userErrors { field message }\n" +
            "  }\n" +
            "}";

    private static final String PRODUCT_VARIANTS_BULK_CREATE_MUTATION = "mutation productVariantsBulkCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!) {\n" +
            "  productVariantsBulkCreate(productId: $productId, variants: $variants) {\n" +
            "    product { id }\n" +
            "    productVariants { id sku }\n" +
            "    userErrors { field message }\n" +
            "  }\n" +
            "}";

    private static final String INVENTORY_SET_ON_HAND_MUTATION = "mutation inventorySetOnHandQuantities($input: InventorySetOnHandQuantitiesInput!) {\n" +
            "  inventorySetOnHandQuantities(input: $input) {\n" +
            "    inventoryAdjustmentGroup { createdAt reason }\n" +
            "    userErrors { field message }\n" +
            "  }\n" +
            "}";

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

    private String numericTail(String value) {
        if (!StringUtils.hasText(value)) {
            return "0";
        }
        int slash = value.lastIndexOf('/');
        String tail = slash >= 0 ? value.substring(slash + 1) : value;
        int q = tail.indexOf('?');
        return q >= 0 ? tail.substring(0, q) : tail;
    }

    private String joinNonBlank(String sep, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                kept.add(p);
            }
        }
        return String.join(sep, kept);
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

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private ShopifySyncResultDto okResult(String message) {
        return ShopifySyncResultDto.builder()
                .success(true)
                .message(message)
                .warnings(new ArrayList<>())
                .build();
    }

    private ShopifySyncResultDto failResult(String message) {
        return ShopifySyncResultDto.builder()
                .success(false)
                .message(message)
                .warnings(new ArrayList<>())
                .build();
    }
}
