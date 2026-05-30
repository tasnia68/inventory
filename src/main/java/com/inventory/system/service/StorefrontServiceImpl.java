package com.inventory.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.Category;
import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.CustomerCategory;
import com.inventory.system.common.entity.CustomerStatus;
import com.inventory.system.common.entity.OrderPriority;
import com.inventory.system.common.entity.ProductImage;
import com.inventory.system.common.entity.ProductVariant;
import com.inventory.system.common.entity.SalesChannel;
import com.inventory.system.common.entity.SalesOrder;
import com.inventory.system.common.entity.SalesOrderItem;
import com.inventory.system.common.entity.SalesOrderStatus;
import com.inventory.system.common.entity.StorefrontPage;
import com.inventory.system.common.entity.StorefrontAccount;
import com.inventory.system.common.entity.StorefrontAccountSession;
import com.inventory.system.common.entity.StorefrontLoginChallenge;
import com.inventory.system.common.entity.StorefrontPublishVersion;
import com.inventory.system.common.entity.Warehouse;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.common.exception.StorefrontModuleDisabledException;
import com.inventory.system.common.exception.StorefrontModuleUnavailableException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.StorefrontAssetUploadDto;
import com.inventory.system.payload.StorefrontAccountAuthDto;
import com.inventory.system.payload.StorefrontAccountOrderDto;
import com.inventory.system.payload.StorefrontAccountOrderItemDto;
import com.inventory.system.payload.StorefrontAccountProfileDto;
import com.inventory.system.payload.StorefrontAccountUpdateRequest;
import com.inventory.system.payload.StorefrontBannerDto;
import com.inventory.system.payload.StorefrontCartDto;
import com.inventory.system.payload.StorefrontCartItemRequest;
import com.inventory.system.payload.StorefrontCartLineDto;
import com.inventory.system.payload.StorefrontCartRequest;
import com.inventory.system.payload.StorefrontAnalyticsDto;
import com.inventory.system.payload.StorefrontCheckoutDto;
import com.inventory.system.payload.StorefrontCheckoutRequest;
import com.inventory.system.payload.StorefrontCmsPageDto;
import com.inventory.system.payload.StorefrontCmsPageRequest;
import com.inventory.system.payload.StorefrontConfigDto;
import com.inventory.system.payload.StorefrontCollectionDto;
import com.inventory.system.payload.StorefrontDomainContextDto;
import com.inventory.system.payload.StorefrontDomainDto;
import com.inventory.system.payload.StorefrontDomainRequest;
import com.inventory.system.payload.StorefrontNavItemDto;
import com.inventory.system.payload.StorefrontLoginChallengeDto;
import com.inventory.system.payload.StorefrontLoginRequest;
import com.inventory.system.payload.StorefrontLoginVerifyRequest;
import com.inventory.system.payload.StorefrontOrderLookupRequest;
import com.inventory.system.payload.StorefrontOrderTrackingDto;
import com.inventory.system.payload.StorefrontPageDto;
import com.inventory.system.payload.StorefrontPublishRequest;
import com.inventory.system.payload.StorefrontPublishVersionDto;
import com.inventory.system.payload.StorefrontProductPageDto;
import com.inventory.system.payload.StorefrontProductDto;
import com.inventory.system.payload.StorefrontSectionDto;
import com.inventory.system.payload.StorefrontSiteDto;
import com.inventory.system.payload.StorefrontThemeDto;
import com.inventory.system.payload.StorefrontThemeBlockDto;
import com.inventory.system.payload.StorefrontThemeDocumentDto;
import com.inventory.system.payload.StorefrontThemeEditorDto;
import com.inventory.system.payload.StorefrontThemeManifestDto;
import com.inventory.system.payload.StorefrontThemeSectionDto;
import com.inventory.system.payload.StorefrontThemeSectionGroupDto;
import com.inventory.system.payload.StorefrontThemeSnapshotDto;
import com.inventory.system.payload.StorefrontThemeTemplateDto;
import com.inventory.system.payload.StorefrontVariantOptionDto;
import com.inventory.system.payload.SalesOrderDto;
import com.inventory.system.payload.SalesOrderItemDto;
import com.inventory.system.payload.SalesOrderItemRequest;
import com.inventory.system.payload.SalesOrderRequest;
import com.inventory.system.payload.StockReservationRequest;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.payload.UpdateStorefrontConfigRequest;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.StorefrontAccountRepository;
import com.inventory.system.repository.StorefrontAccountSessionRepository;
import com.inventory.system.repository.StorefrontLoginChallengeRepository;
import com.inventory.system.repository.StorefrontPageRepository;
import com.inventory.system.repository.StorefrontPublishVersionRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontServiceImpl implements StorefrontService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StorefrontServiceImpl.class);

    private static final String STOREFRONT_MODULE_ENABLED_KEY = "tenant.modules.storefront.enabled";

    private static final String SITE_KEY = "storefront.site";
    private static final String THEME_KEY = "storefront.theme";
    private static final String NAVIGATION_KEY = "storefront.navigation";
    private static final String BANNERS_KEY = "storefront.banners";
    private static final String HOMEPAGE_KEY = "storefront.homepage";
    private static final String DRAFT_THEME_DOCUMENT_KEY = "storefront.themeDocument.draft";
    private static final String THEME_SCHEMA_VERSION_KEY = "storefront.themeSchemaVersion";
    private static final String ACTIVE_REVISION_ID_KEY = "storefront.activeRevisionId";
    private static final String PUBLIC_BASE_URL_KEY = "storefront.publicBaseUrl";
    private static final String CATEGORY = "STOREFRONT";
    private static final String THEME_SCHEMA_VERSION = "v2";
    private static final String SECTION_GROUP_HEADER = "header";
    private static final String SECTION_GROUP_BODY = "body";
    private static final String SECTION_GROUP_FOOTER = "footer";
    private static final Pattern SLUG_NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final TenantSettingService tenantSettingService;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final StorefrontAccountRepository storefrontAccountRepository;
    private final StorefrontLoginChallengeRepository storefrontLoginChallengeRepository;
    private final StorefrontAccountSessionRepository storefrontAccountSessionRepository;
    private final PricingEngineService pricingEngineService;
    private final GiftCardService giftCardService;
    private final org.springframework.context.ApplicationEventPublisher salesEventPublisher;
    private final StockReservationService stockReservationService;
    private final StorefrontPublishVersionRepository storefrontPublishVersionRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final StorefrontDomainService storefrontDomainService;
    private final StorefrontPageRepository storefrontPageRepository;
    private final StorefrontThemeRegistry storefrontThemeRegistry;
    private final org.springframework.transaction.PlatformTransactionManager platformTransactionManager;

    @Override
    @Transactional(readOnly = true)
    public StorefrontConfigDto getAdminConfig() {
        requireAdminStorefrontAccess();
        return enrichWithDomains(deriveLegacyConfig(loadDraftThemeDocument()));
    }

    @Override
    @Transactional
    public StorefrontConfigDto updateConfig(UpdateStorefrontConfigRequest request) {
        requireAdminStorefrontAccess();
        StorefrontConfigDto current = loadConfig();

        StorefrontSiteDto site = request.getSite() != null ? request.getSite() : current.getSite();
        StorefrontThemeDto theme = request.getTheme() != null ? request.getTheme() : current.getTheme();
        List<StorefrontNavItemDto> navigationItems = request.getNavigationItems() != null && !request.getNavigationItems().isEmpty()
                ? request.getNavigationItems()
                : current.getNavigationItems();
        List<StorefrontBannerDto> banners = request.getBanners() != null && !request.getBanners().isEmpty()
                ? request.getBanners()
                : current.getBanners();
        StorefrontPageDto headerPage = request.getHeaderPage() != null ? request.getHeaderPage() : current.getHeaderPage();
        StorefrontPageDto homePage = request.getHomePage() != null ? request.getHomePage() : current.getHomePage();
        StorefrontPageDto footerPage = request.getFooterPage() != null ? request.getFooterPage() : current.getFooterPage();

        StorefrontConfigDto updated = new StorefrontConfigDto(site, theme, navigationItems, banners, headerPage, homePage, footerPage, current.getDomains());
        persistConfig(updated);
        // Patch the legacy site/theme/banners fields into the active draft document,
        // leaving sections/blocks untouched. Older PUT /storefront/config callers
        // only touch brand-level fields; the section editor uses /admin/theme/draft.
        StorefrontThemeDocumentDto draft = loadDraftThemeDocument();
        if (draft.getSettings() == null) draft.setSettings(new LinkedHashMap<>());
        draft.getSettings().put("site", objectMapper.convertValue(site, new TypeReference<Map<String, Object>>() {}));
        draft.getSettings().put("theme", objectMapper.convertValue(theme, new TypeReference<Map<String, Object>>() {}));
        draft.getSettings().put("banners", objectMapper.convertValue(banners, new TypeReference<List<Map<String, Object>>>() {}));
        if (site.getTemplateKey() != null) draft.setTemplateKey(site.getTemplateKey());
        persistDraftThemeDocument(draft);
        return enrichWithDomains(deriveLegacyConfig(loadDraftThemeDocument()));
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontThemeEditorDto getAdminThemeEditor() {
        requireAdminStorefrontAccess();
        StorefrontThemeDocumentDto draft = loadDraftThemeDocument();
        StorefrontThemeDocumentDto published = loadPublishedThemeDocument().orElse(null);
        return new StorefrontThemeEditorDto(
                THEME_SCHEMA_VERSION,
                readStringSetting(ACTIVE_REVISION_ID_KEY, null),
                draft,
                published,
                buildThemeSchema(draft != null ? draft.getTemplateKey() : defaultSite().getTemplateKey()),
                getThemeRevisions(),
                storefrontDomainService.getDomainContextForCurrentTenant()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontDomainContextDto getDomainContext() {
        requireAdminStorefrontAccess();
        return storefrontDomainService.getDomainContextForCurrentTenant();
    }

    @Override
    @Transactional
    public StorefrontDomainDto addDomain(StorefrontDomainRequest request) {
        requireAdminStorefrontAccess();
        return storefrontDomainService.addDomain(request);
    }

    @Override
    @Transactional
    public StorefrontDomainDto verifyDomain(UUID domainId) {
        requireAdminStorefrontAccess();
        return storefrontDomainService.verifyDomain(domainId);
    }

    @Override
    @Transactional
    public StorefrontDomainDto activateDomain(UUID domainId) {
        requireAdminStorefrontAccess();
        return storefrontDomainService.activateDomain(domainId);
    }

    @Override
    @Transactional
    public void removeDomain(UUID domainId) {
        requireAdminStorefrontAccess();
        storefrontDomainService.removeDomain(domainId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allowCaddyDomain(String domain) {
        if (!storefrontDomainService.isDomainAllowedForCaddy(domain)) {
            return false;
        }

        Optional<String> tenantId = storefrontDomainService.resolveTenantIdForHost(domain);
        if (tenantId.isEmpty()) {
            return true;
        }

        return isStorefrontModuleEnabledForTenant(tenantId.get());
    }

    @Override
    @Transactional
    public StorefrontThemeDocumentDto updateDraftTheme(StorefrontThemeDocumentDto request) {
        requireAdminStorefrontAccess();
        StorefrontThemeDocumentDto normalized = normalizeThemeDocument(request);
        persistDraftThemeDocument(normalized);
        persistConfig(deriveLegacyConfig(normalized));
        return normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontPublishVersionDto> getThemeRevisions() {
        requireAdminStorefrontAccess();
        return getPublishVersions();
    }

    @Override
    @Transactional
    public StorefrontPublishVersionDto publishDraftTheme(StorefrontPublishRequest request) {
        requireAdminStorefrontAccess();
        return publishCurrentConfig(request);
    }

    @Override
    @Transactional
    public StorefrontPublishVersionDto restoreThemeRevision(UUID versionId, StorefrontPublishRequest request) {
        requireAdminStorefrontAccess();
        return rollbackToVersion(versionId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontConfigDto getAdminThemePreview() {
        requireAdminStorefrontAccess();
        return enrichWithDomains(deriveLegacyConfig(loadDraftThemeDocument()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontThemeManifestDto> listAvailableThemes() {
        requireAdminStorefrontAccess();
        return storefrontThemeRegistry.listAll();
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontThemeManifestDto getThemeManifest(String themeKey) {
        requireAdminStorefrontAccess();
        return storefrontThemeRegistry.findByKey(themeKey)
                .orElseThrow(() -> new ResourceNotFoundException("Theme manifest not found: " + themeKey));
    }

    @Override
    @Transactional(readOnly = true)
    public com.inventory.system.payload.StorefrontThemeUpgradeStatusDto getActiveThemeUpgradeStatus() {
        requireAdminStorefrontAccess();
        StorefrontThemeDocumentDto draft = loadDraftThemeDocument();
        String templateKey = draft != null && draft.getTemplateKey() != null
                ? draft.getTemplateKey()
                : defaultSite().getTemplateKey();
        StorefrontThemeManifestDto manifest = storefrontThemeRegistry.findByKey(templateKey).orElse(null);
        if (manifest == null) {
            return new com.inventory.system.payload.StorefrontThemeUpgradeStatusDto(
                    templateKey, templateKey, null, null, false, null, null);
        }
        StorefrontPublishVersion latest = storefrontPublishVersionRepository.findTopByOrderByVersionNumberDesc().orElse(null);
        String activeVersion = latest != null ? latest.getThemeVersion() : null;
        String availableVersion = manifest.getVersion();
        // Treat the manifest as authoritative — published version null/missing means
        // the tenant hasn't published since theme versioning was introduced, so
        // treat that as "needs publish" only if versions actually differ.
        boolean hasUpgrade = availableVersion != null && !availableVersion.equals(activeVersion);
        return new com.inventory.system.payload.StorefrontThemeUpgradeStatusDto(
                manifest.getKey(),
                manifest.getName(),
                activeVersion,
                availableVersion,
                hasUpgrade,
                latest != null && latest.getPublishedAt() != null ? latest.getPublishedAt().toString() : null,
                manifest.getMigrations()
        );
    }

    @Override
    @Transactional
    public StorefrontThemeEditorDto applyActiveThemeUpgrade() {
        requireAdminStorefrontAccess();
        StorefrontThemeDocumentDto draft = loadDraftThemeDocument();
        String templateKey = draft != null && draft.getTemplateKey() != null
                ? draft.getTemplateKey()
                : defaultSite().getTemplateKey();
        return activateTheme(templateKey);
    }

    @Override
    @Transactional
    public StorefrontThemeEditorDto activateTheme(String themeKey) {
        requireAdminStorefrontAccess();
        StorefrontThemeManifestDto manifest = storefrontThemeRegistry.findByKey(themeKey)
                .orElseThrow(() -> new ResourceNotFoundException("Theme manifest not found: " + themeKey));

        StorefrontThemeDocumentDto draft = loadDraftThemeDocument();
        if (draft == null) {
            draft = new StorefrontThemeDocumentDto();
        }
        draft.setTemplateKey(manifest.getKey());
        if (draft.getSettings() == null) {
            draft.setSettings(new LinkedHashMap<>());
        }
        Map<String, Object> defaults = manifest.getDefaultSettings() != null
                ? manifest.getDefaultSettings()
                : new LinkedHashMap<>();
        // Site group: preserve user values (brand name, domain, SEO) — only fill blanks.
        mergeSettingsGroup(draft.getSettings(), defaults, "site");
        // Theme tokens (colors / fonts / radius): switching themes should give the new
        // theme's visual identity, so overwrite rather than merge.
        if (defaults.get("theme") instanceof Map<?, ?>) {
            draft.getSettings().put("theme", new LinkedHashMap<>((Map<String, Object>) defaults.get("theme")));
        }
        Object siteObj = draft.getSettings().get("site");
        if (siteObj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> siteMap = (Map<String, Object>) siteObj;
            siteMap.put("templateKey", manifest.getKey());
        } else {
            Map<String, Object> siteMap = new LinkedHashMap<>();
            siteMap.put("templateKey", manifest.getKey());
            draft.getSettings().put("site", siteMap);
        }

        StorefrontThemeDocumentDto normalized = normalizeThemeDocument(draft);
        persistDraftThemeDocument(normalized);
        persistConfig(deriveLegacyConfig(normalized));
        return getAdminThemeEditor();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontAccountProfileDto> listStorefrontAccounts() {
        return storefrontAccountRepository.findAll().stream()
                .map(this::mapStorefrontAccountProfile)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontAnalyticsDto getStorefrontAnalytics(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now();

        List<SalesOrder> allOrders = salesOrderRepository.findAll().stream()
                .filter(o -> o.getSalesChannel() == SalesChannel.WEB_ORDER)
                .filter(o -> o.getOrderDate() != null && !o.getOrderDate().isBefore(fromDt) && o.getOrderDate().isBefore(toDt))
                .toList();

        long totalOrders = allOrders.size();
        BigDecimal totalRevenue = allOrders.stream().map(SalesOrder::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageOrderValue = totalOrders > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        long newCustomers = storefrontAccountRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(fromDt) && a.getCreatedAt().isBefore(toDt))
                .count();

        List<StorefrontAnalyticsDto.StatusBreakdown> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new StorefrontAnalyticsDto.StatusBreakdown(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        List<StorefrontAnalyticsDto.TopProduct> topProducts = allOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProductVariant().getSku(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(e -> {
                    String sku = e.getKey();
                    String productName = e.getValue().stream()
                            .map(item -> item.getProductVariant().getTemplate() != null ? item.getProductVariant().getTemplate().getName() : sku)
                            .findFirst().orElse(sku);
                    BigDecimal qty = e.getValue().stream().map(SalesOrderItem::getQuantity).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal rev = e.getValue().stream().map(SalesOrderItem::getTotalPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new StorefrontAnalyticsDto.TopProduct(productName, sku, qty, rev);
                })
                .sorted((a, b) -> b.getTotalQuantity().compareTo(a.getTotalQuantity()))
                .limit(5)
                .collect(Collectors.toList());

        List<StorefrontAnalyticsDto.DailyRevenue> dailyRevenue = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getOrderDate().toLocalDate()))
                .entrySet().stream()
                .map(e -> new StorefrontAnalyticsDto.DailyRevenue(
                        e.getKey().toString(),
                        e.getValue().stream().map(SalesOrder::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()
                ))
                .sorted(Comparator.comparing(StorefrontAnalyticsDto.DailyRevenue::getDate))
                .collect(Collectors.toList());

        return new StorefrontAnalyticsDto(totalOrders, totalRevenue, averageOrderValue, newCustomers, ordersByStatus, topProducts, dailyRevenue);
    }

    // ── CMS Pages ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontCmsPageDto> listCmsPages() {
        return storefrontPageRepository.findAll().stream().map(this::mapCmsPage).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontCmsPageDto getCmsPage(UUID id) {
        return mapCmsPage(storefrontPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StorefrontPage", "id", id)));
    }

    @Override
    @Transactional
    public StorefrontCmsPageDto createCmsPage(StorefrontCmsPageRequest request) {
        String slug = request.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        if (storefrontPageRepository.existsBySlug(slug)) {
            throw new BadRequestException("A page with slug '" + slug + "' already exists");
        }
        StorefrontPage page = new StorefrontPage();
        page.setTitle(request.getTitle().trim());
        page.setSlug(slug);
        page.setBody(request.getBody());
        page.setPublished(request.isPublished());
        return mapCmsPage(storefrontPageRepository.save(page));
    }

    @Override
    @Transactional
    public StorefrontCmsPageDto updateCmsPage(UUID id, StorefrontCmsPageRequest request) {
        StorefrontPage page = storefrontPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StorefrontPage", "id", id));
        String slug = request.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        storefrontPageRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("A page with slug '" + slug + "' already exists");
            }
        });
        page.setTitle(request.getTitle().trim());
        page.setSlug(slug);
        page.setBody(request.getBody());
        page.setPublished(request.isPublished());
        return mapCmsPage(storefrontPageRepository.save(page));
    }

    @Override
    @Transactional
    public void deleteCmsPage(UUID id) {
        StorefrontPage page = storefrontPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StorefrontPage", "id", id));
        storefrontPageRepository.delete(page);
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontCmsPageDto getPublicCmsPage(String slug) {
        requirePublicStorefrontAccess();
        StorefrontPage page = storefrontPageRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("StorefrontPage", "slug", slug));
        return mapCmsPage(page);
    }

    private StorefrontCmsPageDto mapCmsPage(StorefrontPage page) {
        return new StorefrontCmsPageDto(page.getId(), page.getTitle(), page.getSlug(), page.getBody(), page.isPublished(), page.getCreatedAt(), page.getUpdatedAt());
    }

    @Override
    @Transactional
    public StorefrontLoginChallengeDto requestAccountLogin(StorefrontLoginRequest request) {
        requirePublicStorefrontAccess();
        String email = request.getEmail().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        storefrontLoginChallengeRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .filter(challenge -> challenge.getConsumedAt() == null)
                .forEach(challenge -> challenge.setConsumedAt(now));

        StorefrontLoginChallenge challenge = new StorefrontLoginChallenge();
        challenge.setEmail(email);
        challenge.setOtpCode(generateOtpCode());
        challenge.setMagicToken(UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", ""));
        challenge.setExpiresAt(now.plusMinutes(15));
        StorefrontLoginChallenge saved = storefrontLoginChallengeRepository.save(challenge);

        emailService.sendStorefrontLoginEmail(email, saved.getOtpCode(), buildStorefrontMagicLink(saved.getMagicToken()));
        return new StorefrontLoginChallengeDto(email, saved.getExpiresAt().toString());
    }

    @Override
    @Transactional
    public StorefrontAccountAuthDto verifyAccountLogin(StorefrontLoginVerifyRequest request) {
        requirePublicStorefrontAccess();
        StorefrontLoginChallenge challenge = resolveLoginChallenge(request);
        if (challenge.getConsumedAt() != null || challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This storefront login code has expired");
        }
        challenge.setConsumedAt(LocalDateTime.now());

        StorefrontAccount account = resolveOrCreateStorefrontAccount(challenge.getEmail());
        account.setLastLoginAt(LocalDateTime.now());
        if (account.getEmailVerifiedAt() == null) {
            account.setEmailVerifiedAt(LocalDateTime.now());
        }
        StorefrontAccount savedAccount = storefrontAccountRepository.save(account);

        StorefrontAccountSession session = new StorefrontAccountSession();
        session.setStorefrontAccount(savedAccount);
        session.setSessionToken(UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", ""));
        session.setLastSeenAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30));
        StorefrontAccountSession savedSession = storefrontAccountSessionRepository.save(session);

        return new StorefrontAccountAuthDto(
                savedSession.getSessionToken(),
                savedSession.getExpiresAt().toString(),
                mapStorefrontAccountProfile(savedAccount)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontAccountProfileDto getAccountProfile(String sessionToken) {
        requirePublicStorefrontAccess();
        return mapStorefrontAccountProfile(requireStorefrontSession(sessionToken).getStorefrontAccount());
    }

    @Override
    @Transactional
    public StorefrontAccountProfileDto updateAccountProfile(String sessionToken, StorefrontAccountUpdateRequest request) {
        requirePublicStorefrontAccess();
        StorefrontAccountSession session = requireStorefrontSession(sessionToken);
        StorefrontAccount account = session.getStorefrontAccount();
        if (request.getName() != null) {
            account.setName(blankToNull(request.getName()));
        }
        if (request.getAddress() != null) {
            account.setAddress(blankToNull(request.getAddress()));
        }

        Customer customer = account.getCustomer();
        if (request.getName() != null && account.getName() != null) {
            customer.setName(account.getName());
            customer.setContactName(account.getName());
        }
        if (request.getAddress() != null) {
            customer.setAddress(account.getAddress());
        }
        customerRepository.save(customer);
        return mapStorefrontAccountProfile(storefrontAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontAccountOrderDto> getAccountOrders(String sessionToken) {
        requirePublicStorefrontAccess();
        StorefrontAccount account = requireStorefrontSession(sessionToken).getStorefrontAccount();
        return salesOrderRepository.findByCustomerId(account.getCustomer().getId(), PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "orderDate")))
                .stream()
                .map(order -> new StorefrontAccountOrderDto(
                        order.getSoNumber(),
                        order.getStatus() != null ? order.getStatus().name() : null,
                        order.getOrderDate() != null ? order.getOrderDate().toString() : null,
                        order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().toString() : null,
                        order.getTotalAmount(),
                        order.getCurrency(),
                        order.getItems().stream()
                                .map(item -> new StorefrontAccountOrderItemDto(
                                        item.getProductVariant() != null ? item.getProductVariant().getSku() : null,
                                        item.getProductVariant() != null ? item.getProductVariant().getTemplate().getName() : null,
                                        item.getQuantity(),
                                        item.getTotalPrice()
                                ))
                                .toList(),
                        java.util.List.of()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontAccountOrderDto getAccountOrderDetail(String sessionToken, String orderNumber) {
        requirePublicStorefrontAccess();
        StorefrontAccount account = requireStorefrontSession(sessionToken).getStorefrontAccount();
        SalesOrder order = salesOrderRepository.findBySoNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        if (!order.getCustomer().getId().equals(account.getCustomer().getId())) {
            throw new BadRequestException("Order does not belong to this account");
        }
        return new StorefrontAccountOrderDto(
                order.getSoNumber(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getOrderDate() != null ? order.getOrderDate().toString() : null,
                order.getExpectedDeliveryDate() != null ? order.getExpectedDeliveryDate().toString() : null,
                order.getTotalAmount(),
                order.getCurrency(),
                order.getItems().stream()
                        .map(item -> new StorefrontAccountOrderItemDto(
                                item.getProductVariant() != null ? item.getProductVariant().getSku() : null,
                                item.getProductVariant() != null ? item.getProductVariant().getTemplate().getName() : null,
                                item.getQuantity(),
                                item.getTotalPrice()
                        ))
                        .toList(),
                java.util.List.of()
        );
    }

    @Override
    @Transactional
    public void logoutAccount(String sessionToken) {
        requirePublicStorefrontAccess();
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        storefrontAccountSessionRepository.findFirstBySessionToken(sessionToken.trim())
                .ifPresent(session -> {
                    session.setRevokedAt(LocalDateTime.now());
                    storefrontAccountSessionRepository.save(session);
                });
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public StorefrontConfigDto getPublicConfig() {
        requirePublicStorefrontAccess();
        try {
            return storefrontPublishVersionRepository.findTopByOrderByVersionNumberDesc()
                    .map(this::readThemeSnapshot)
                    .map(StorefrontThemeSnapshotDto::getConfig)
                    .map(this::enrichWithDomains)
                    .orElseGet(() -> enrichWithDomains(deriveLegacyConfig(loadDraftThemeDocument())));
        } catch (RuntimeException ignored) {
            return enrichWithDomains(deriveLegacyConfig(loadDraftThemeDocument()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontCollectionDto> getPublicCollections() {
        requirePublicStorefrontAccess();
        List<Category> publishedCategories = categoryRepository.findByPublishedToStorefrontTrueOrderByStorefrontSortOrderAscNameAsc();
        Map<UUID, List<Category>> childrenByParent = new LinkedHashMap<>();
        for (Category category : publishedCategories) {
            UUID parentId = category.getParent() != null && Boolean.TRUE.equals(category.getParent().getPublishedToStorefront())
                    ? category.getParent().getId()
                    : null;
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }

        return childrenByParent.getOrDefault(null, List.of()).stream()
                .map(category -> mapCollection(category, childrenByParent, 0))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontProductPageDto getPublicProducts(String query, String collectionSlug, String sort, Integer page, Integer size) {
        requirePublicStorefrontAccess();
        int pageNumber = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 48) : 24;
        List<ProductVariant> publicVariants = loadPublishedStorefrontVariants();
        Map<UUID, List<ProductVariant>> siblingsByTemplate = buildSiblingsByTemplate(publicVariants);
        Warehouse storefrontWarehouse = resolveStorefrontWarehouse();
        List<StorefrontProductDto> products = siblingsByTemplate.values().stream()
                .map(this::resolvePrimaryVariant)
                .filter(Objects::nonNull)
                .map(variant -> mapProduct(variant, siblingsByTemplate, storefrontWarehouse, true))
                .toList();

        List<StorefrontProductDto> filteredProducts = products.stream()
                .filter(product -> matchesQuery(product, query))
                .filter(product -> matchesCollection(product, collectionSlug))
                .sorted(productComparator(sort))
                .toList();

        int fromIndex = Math.min(pageNumber * pageSize, filteredProducts.size());
        int toIndex = Math.min(fromIndex + pageSize, filteredProducts.size());
        List<StorefrontProductDto> items = filteredProducts.subList(fromIndex, toIndex);
        int totalPages = filteredProducts.isEmpty() ? 0 : (int) Math.ceil((double) filteredProducts.size() / pageSize);

        return new StorefrontProductPageDto(
                items,
                pageNumber,
                pageSize,
                filteredProducts.size(),
                totalPages,
                pageNumber + 1 < totalPages,
                pageNumber > 0 && totalPages > 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontProductDto getPublicProduct(String slug) {
        requirePublicStorefrontAccess();
        List<ProductVariant> publicVariants = loadPublishedStorefrontVariants();
        Map<UUID, List<ProductVariant>> siblingsByTemplate = buildSiblingsByTemplate(publicVariants);
        Warehouse storefrontWarehouse = resolveStorefrontWarehouse();

        return publicVariants.stream()
                .filter(variant -> {
                    List<ProductVariant> siblings = siblingsByTemplate.getOrDefault(variant.getTemplate().getId(), List.of());
                    String templateSlug = buildTemplateProductSlug(variant);
                    String variantSlug = buildProductSlug(variant, siblings);
                    return templateSlug.equalsIgnoreCase(slug) || variantSlug.equalsIgnoreCase(slug);
                })
                .findFirst()
                .map(variant -> mapProduct(variant, siblingsByTemplate, storefrontWarehouse, false))
                .orElseThrow(() -> new ResourceNotFoundException("Storefront product", "slug", slug));
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontCartDto previewCart(StorefrontCartRequest request) {
        requirePublicStorefrontAccess();
        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        Customer customer = resolvePricingCustomer(request.getCustomerId(), request.getCustomerEmail(), request.getCustomerPhoneNumber());
        SalesOrderRequest pricingRequest = toSalesOrderRequest(request.getItems(), warehouse.getId(), request.getCurrency(), request.getCouponCodes(), request.getGiftCardCodes(), request.getReferralCode(), customer, request.getExpectedDeliveryDate());
        PricingEvaluation pricingEvaluation = pricingEngineService.evaluateSalesOrder(customer, warehouse, pricingRequest);

        return toCartDto(warehouse, pricingEvaluation, request.getCurrency());
    }

    @Override
    @Transactional
    public StorefrontCheckoutDto checkout(StorefrontCheckoutRequest request) {
        requirePublicStorefrontAccess();
        if ((request.getCustomerEmail() == null || request.getCustomerEmail().isBlank())
                && (request.getCustomerPhoneNumber() == null || request.getCustomerPhoneNumber().isBlank())) {
            throw new BadRequestException("Customer email or phone number is required for storefront checkout");
        }

        Warehouse warehouse = resolveWarehouse(request.getWarehouseId());
        Customer customer = resolveOrCreateCheckoutCustomer(request);
        validateCustomerEligibility(customer);
        LocalDate expectedDeliveryDate = request.getExpectedDeliveryDate() != null ? request.getExpectedDeliveryDate() : LocalDate.now().plusDays(3);

        SalesOrderRequest pricingRequest = toSalesOrderRequest(request.getItems(), warehouse.getId(), request.getCurrency(), request.getCouponCodes(), request.getGiftCardCodes(), request.getReferralCode(), customer, expectedDeliveryDate);
        PricingEvaluation pricingEvaluation = pricingEngineService.evaluateSalesOrder(customer, warehouse, pricingRequest);
        Map<UUID, ProductVariant> variantMap = loadVariants(request.getItems());
        Map<UUID, Deque<PricingEvaluationLine>> pricedLines = buildLineQueues(pricingEvaluation);

        BigDecimal shippingAmount = computeShippingAmount(pricingEvaluation.getNetSubtotal());
        BigDecimal taxAmount = computeTaxAmount(pricingEvaluation.getNetSubtotal());
        BigDecimal beforeGift = pricingEvaluation.getNetSubtotal().add(shippingAmount).add(taxAmount);
        BigDecimal grandTotal = beforeGift;

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomer(customer);
        salesOrder.setWarehouse(warehouse);
        salesOrder.setOrderDate(LocalDateTime.now());
        salesOrder.setExpectedDeliveryDate(expectedDeliveryDate);
        salesOrder.setStatus(SalesOrderStatus.PENDING);
        salesOrder.setPriority(OrderPriority.HIGH);
        salesOrder.setSoNumber(generateStorefrontOrderNumber());
        salesOrder.setSalesChannel(SalesChannel.WEB_ORDER);
        salesOrder.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "BDT");
        salesOrder.setNotes(buildStorefrontOrderNotes(request));
        salesOrder.setSubtotalAmount(pricingEvaluation.getBaseSubtotal());
        salesOrder.setDiscountAmount(pricingEvaluation.getTotalDiscount());
        salesOrder.setShippingAmount(shippingAmount);
        salesOrder.setTaxAmount(taxAmount);
        salesOrder.setTotalAmount(grandTotal);
        salesOrder.setAppliedCouponCodes(String.join(", ", pricingEvaluation.getAppliedCouponCodes()));
        salesOrder.setReferralCode(request.getReferralCode());

        List<SalesOrderItem> items = new ArrayList<>();
        for (StorefrontCartItemRequest itemRequest : request.getItems()) {
            ProductVariant productVariant = variantMap.get(itemRequest.getProductVariantId());
            PricingEvaluationLine pricedLine = popPricedLine(pricedLines, productVariant.getId());

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrder(salesOrder);
            item.setProductVariant(productVariant);
            item.setQuantity(itemRequest.getQuantity());
            item.setBaseUnitPrice(pricedLine.getBaseUnitPrice());
            item.setUnitPrice(pricedLine.getFinalUnitPrice());
            item.setLineDiscount(pricedLine.getLineDiscountAmount());
            item.setAppliedPromotionCodes(String.join(", ", pricedLine.getAppliedPromotionCodes()));
            item.setTotalPrice(pricedLine.getLineTotalAmount());
            item.setShippedQuantity(BigDecimal.ZERO);
            items.add(item);
        }
        salesOrder.setItems(items);

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        pricingEngineService.recordRedemptions(pricingEvaluation, savedOrder, null, customer, SalesChannel.WEB_ORDER, savedOrder.getSoNumber());

        if (request.getGiftCardCodes() != null && !request.getGiftCardCodes().isEmpty()) {
            BigDecimal redeemed = giftCardService.redeemCodes(
                    request.getGiftCardCodes(),
                    beforeGift,
                    savedOrder.getId(),
                    null,
                    savedOrder.getSoNumber()
            );
            if (redeemed != null && redeemed.signum() > 0) {
                savedOrder.setGiftCardAmount(redeemed);
                savedOrder.setAppliedGiftCardCodes(String.join(", ", request.getGiftCardCodes()));
                savedOrder.setTotalAmount(beforeGift.subtract(redeemed));
                savedOrder = salesOrderRepository.save(savedOrder);
            }
        }

        salesEventPublisher.publishEvent(new com.inventory.system.service.order.events.SalesOrderCreatedEvent(
                savedOrder.getId(),
                customer != null ? customer.getId() : null,
                SalesChannel.WEB_ORDER,
                request.getReferralCode(),
                savedOrder.getTotalAmount(),
                LocalDateTime.now()
        ));

        reserveStockForOrder(savedOrder, warehouse);

        // Send order confirmation email
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            try {
                emailService.sendOrderConfirmationEmail(
                        customer.getEmail(),
                        customer.getName(),
                        savedOrder.getSoNumber(),
                        savedOrder.getTotalAmount() != null ? savedOrder.getTotalAmount().toPlainString() : "0.00",
                        savedOrder.getCurrency() != null ? savedOrder.getCurrency() : "BDT"
                );
            } catch (Exception e) {
                // Log but don't fail the checkout
                logger.warn("Failed to send order confirmation email for {}: {}", savedOrder.getSoNumber(), e.getMessage());
            }
        }

        return new StorefrontCheckoutDto(
                "PENDING_APPROVAL",
                "Storefront order created and queued for backoffice review.",
                mapSalesOrder(savedOrder)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontOrderTrackingDto lookupOrder(StorefrontOrderLookupRequest request) {
        requirePublicStorefrontAccess();
        String orderNumber = request.getOrderNumber().trim();
        if ((request.getCustomerEmail() == null || request.getCustomerEmail().isBlank())
                && (request.getCustomerPhoneNumber() == null || request.getCustomerPhoneNumber().isBlank())) {
            throw new BadRequestException("Customer email or phone number is required for storefront order lookup");
        }

        Optional<SalesOrder> order = Optional.empty();
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            order = salesOrderRepository.findBySoNumberAndCustomerEmailIgnoreCase(orderNumber, request.getCustomerEmail().trim());
        }
        if (order.isEmpty() && request.getCustomerPhoneNumber() != null && !request.getCustomerPhoneNumber().isBlank()) {
            order = salesOrderRepository.findBySoNumberAndCustomerPhoneNumber(orderNumber, request.getCustomerPhoneNumber().trim());
        }

        SalesOrder salesOrder = order
                .filter(found -> found.getSoNumber() != null && found.getSoNumber().startsWith("SO-WEB-"))
                .orElseThrow(() -> new ResourceNotFoundException("Storefront order", "orderNumber", orderNumber));

        return new StorefrontOrderTrackingDto(
                salesOrder.getSoNumber(),
                salesOrder.getStatus(),
                salesOrder.getCustomer().getName(),
                salesOrder.getCustomer().getEmail(),
                salesOrder.getCustomer().getPhoneNumber(),
                salesOrder.getWarehouse() != null ? salesOrder.getWarehouse().getName() : null,
                salesOrder.getOrderDate(),
                salesOrder.getExpectedDeliveryDate(),
                salesOrder.getTotalAmount(),
                salesOrder.getCurrency(),
                salesOrder.getItems().stream().map(this::mapSalesOrderItem).toList(),
                java.util.List.of()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorefrontPublishVersionDto> getPublishVersions() {
        requireAdminStorefrontAccess();
        return storefrontPublishVersionRepository.findAllByOrderByVersionNumberDesc().stream()
                .map(this::mapPublishVersion)
                .toList();
    }

    @Override
    @Transactional
    public StorefrontPublishVersionDto publishCurrentConfig(StorefrontPublishRequest request) {
        requireAdminStorefrontAccess();
        StorefrontThemeDocumentDto currentTheme = loadDraftThemeDocument();
        StorefrontConfigDto current = deriveLegacyConfig(currentTheme);
        int nextVersionNumber = storefrontPublishVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        LocalDateTime publishedAt = LocalDateTime.now();

        current.getSite().setPublishStatus("PUBLISHED");
        current.getSite().setPublishedVersion("Publish " + nextVersionNumber);
        current.getSite().setLastPublishedAt(publishedAt.toString());
        persistConfig(current);

        StorefrontPublishVersion version = new StorefrontPublishVersion();
        version.setVersionNumber(nextVersionNumber);
        version.setPublishedAt(publishedAt);
        version.setNote(request != null ? request.getNote() : null);
        version.setSnapshotJson(writeThemeSnapshot(new StorefrontThemeSnapshotDto(
                THEME_SCHEMA_VERSION,
                null,
                currentTheme,
                current
        )));
        stampThemeMetadata(version, currentTheme);

        StorefrontPublishVersion saved = storefrontPublishVersionRepository.save(version);
        writeStringSetting(ACTIVE_REVISION_ID_KEY, saved.getId().toString());
        return mapPublishVersion(saved);
    }

    @Override
    @Transactional
    public StorefrontPublishVersionDto rollbackToVersion(UUID versionId, StorefrontPublishRequest request) {
        requireAdminStorefrontAccess();
        StorefrontPublishVersion targetVersion = storefrontPublishVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront publish version", "id", versionId));

        StorefrontThemeSnapshotDto snapshot = readThemeSnapshot(targetVersion);
        int nextVersionNumber = storefrontPublishVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        LocalDateTime publishedAt = LocalDateTime.now();

        snapshot.getConfig().getSite().setPublishStatus("PUBLISHED");
        snapshot.getConfig().getSite().setPublishedVersion("Publish " + nextVersionNumber);
        snapshot.getConfig().getSite().setLastPublishedAt(publishedAt.toString());
        persistDraftThemeDocument(snapshot.getThemeDocument());
        persistConfig(snapshot.getConfig());

        StorefrontPublishVersion rollbackVersion = new StorefrontPublishVersion();
        rollbackVersion.setVersionNumber(nextVersionNumber);
        rollbackVersion.setPublishedAt(publishedAt);
        rollbackVersion.setRestoredFromVersionNumber(targetVersion.getVersionNumber());
        rollbackVersion.setNote(request != null && request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote()
                : "Rollback to Publish " + targetVersion.getVersionNumber());
        rollbackVersion.setSnapshotJson(writeThemeSnapshot(new StorefrontThemeSnapshotDto(
                THEME_SCHEMA_VERSION,
                versionId.toString(),
                snapshot.getThemeDocument(),
                snapshot.getConfig()
        )));
        stampThemeMetadata(rollbackVersion, snapshot.getThemeDocument());

        StorefrontPublishVersion saved = storefrontPublishVersionRepository.save(rollbackVersion);
        writeStringSetting(ACTIVE_REVISION_ID_KEY, saved.getId().toString());
        return mapPublishVersion(saved);
    }

    @Override
    @Transactional
    public StorefrontAssetUploadDto uploadAsset(MultipartFile file, String assetType) {
        requireAdminStorefrontAccess();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A storefront asset file is required");
        }

        String normalizedAssetType = (assetType == null || assetType.isBlank())
                ? "misc"
                : assetType.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
        String tenantId = TenantContext.getTenantId();
        String folder = "storefront-assets/" + (tenantId != null && !tenantId.isBlank() ? tenantId : "default-tenant") + "/" + normalizedAssetType;
        String storagePath = fileStorageService.uploadFile(file, folder);
        String publicUrl = "/api/v1/storefront/assets/file?path="
                + URLEncoder.encode(storagePath, StandardCharsets.UTF_8);

        return new StorefrontAssetUploadDto(
                normalizedAssetType,
                file.getOriginalFilename(),
                storagePath,
                publicUrl
        );
    }

    private StorefrontConfigDto loadConfig() {
        Optional<StorefrontThemeDocumentDto> themeDocument = readThemeDocumentSetting(DRAFT_THEME_DOCUMENT_KEY);
        if (themeDocument.isPresent()) {
            return enrichWithDomains(deriveLegacyConfig(themeDocument.get()));
        }
        StorefrontThemeDocumentDto fresh = buildDefaultThemeDocument();
        persistDraftThemeDocument(fresh);
        return enrichWithDomains(deriveLegacyConfig(fresh));
    }

    private void requireAdminStorefrontAccess() {
        if (!isStorefrontModuleEnabled()) {
            throw new StorefrontModuleDisabledException();
        }
    }

    private void requirePublicStorefrontAccess() {
        if (!isStorefrontModuleEnabled()) {
            throw new StorefrontModuleUnavailableException();
        }
    }

    private boolean isStorefrontModuleEnabled() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return isStorefrontModuleEnabledForTenant(tenantId);
    }

    private boolean isStorefrontModuleEnabledForTenant(String tenantId) {
        return tenantSettingRepository.findByTenantIdAndSettingKey(tenantId, STOREFRONT_MODULE_ENABLED_KEY)
            .map(setting -> setting.getSettingValue())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private void persistConfig(StorefrontConfigDto config) {
        writeJsonSetting(SITE_KEY, config.getSite());
        writeJsonSetting(THEME_KEY, config.getTheme());
        writeJsonSetting(NAVIGATION_KEY, config.getNavigationItems());
        writeJsonSetting(BANNERS_KEY, config.getBanners());
        writeJsonSetting(HOMEPAGE_KEY, config.getHomePage());
    }

    private <T> T readSetting(String key, TypeReference<T> typeReference, T fallback) {
        Optional<TenantSettingDto> setting = tenantSettingService.findSetting(key);
        if (setting.isEmpty() || setting.get().getValue() == null || setting.get().getValue().isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(setting.get().getValue(), typeReference);
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private void writeJsonSetting(String key, Object value) {
        try {
            tenantSettingService.updateSetting(key, objectMapper.writeValueAsString(value), "JSON", CATEGORY);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to persist storefront setting " + key, exception);
        }
    }

    private void writeStringSetting(String key, String value) {
        tenantSettingService.updateSetting(key, value, "STRING", CATEGORY);
    }

    private String readStringSetting(String key, String fallback) {
        return tenantSettingService.findSetting(key)
                .map(TenantSettingDto::getValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateOtpCode() {
        int value = 100000 + (int) (Math.random() * 900000);
        return String.valueOf(value);
    }

    private String buildStorefrontMagicLink(String magicToken) {
        String configured = readStringSetting(PUBLIC_BASE_URL_KEY, null);
        String baseUrl;
        if (configured != null && !configured.isBlank()) {
            baseUrl = configured.trim().replaceAll("/+$", "");
        } else if (storefrontDomainService.getPrimaryStorefrontUrlForCurrentTenant().isPresent()) {
            baseUrl = storefrontDomainService.getPrimaryStorefrontUrlForCurrentTenant().orElseThrow();
        } else {
            baseUrl = "http://localhost:5173";
        }
        return baseUrl + "/account/verify?token=" + URLEncoder.encode(magicToken, StandardCharsets.UTF_8);
    }

    private StorefrontLoginChallenge resolveLoginChallenge(StorefrontLoginVerifyRequest request) {
        if (request.getMagicToken() != null && !request.getMagicToken().isBlank()) {
            return storefrontLoginChallengeRepository.findFirstByMagicToken(request.getMagicToken().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Storefront login challenge", "magicToken", request.getMagicToken()));
        }
        if (request.getEmail() == null || request.getEmail().isBlank() || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new BadRequestException("Email and OTP code are required");
        }
        return storefrontLoginChallengeRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(request.getEmail().trim().toLowerCase()).stream()
                .filter(challenge -> challenge.getConsumedAt() == null)
                .filter(challenge -> challenge.getOtpCode().equals(request.getOtpCode().trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid storefront login code"));
    }

    private StorefrontAccount resolveOrCreateStorefrontAccount(String email) {
        Optional<StorefrontAccount> existingAccount = storefrontAccountRepository.findFirstByEmailIgnoreCase(email);
        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }

        Customer customer = customerRepository.findFirstByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    Customer created = new Customer();
                    created.setName("Storefront Customer");
                    created.setContactName(null);
                    created.setEmail(email);
                    created.setPhoneNumber(null);
                    created.setAddress(null);
                    created.setCategory(CustomerCategory.OTHER);
                    created.setStatus(CustomerStatus.ACTIVE);
                    created.setIsActive(true);
                    return customerRepository.save(created);
                });

        StorefrontAccount linkedAccount = storefrontAccountRepository.findFirstByCustomerId(customer.getId()).orElse(null);
        if (linkedAccount != null) {
            if (linkedAccount.getEmail() == null || linkedAccount.getEmail().isBlank()) {
                linkedAccount.setEmail(email);
            }
            return linkedAccount;
        }

        StorefrontAccount account = new StorefrontAccount();
        account.setCustomer(customer);
        account.setEmail(email);
        account.setName(blankToNull(customer.getName()));
        account.setAddress(blankToNull(customer.getAddress()));
        return account;
    }

    private StorefrontAccountSession requireStorefrontSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new BadRequestException("Storefront session token is required");
        }
        StorefrontAccountSession session = storefrontAccountSessionRepository.findFirstBySessionToken(sessionToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Storefront session", "token", sessionToken));
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Storefront session has expired");
        }
        session.setLastSeenAt(LocalDateTime.now());
        storefrontAccountSessionRepository.save(session);
        return session;
    }

    private StorefrontAccountProfileDto mapStorefrontAccountProfile(StorefrontAccount account) {
        return new StorefrontAccountProfileDto(
                account.getId().toString(),
                account.getCustomer() != null ? account.getCustomer().getId().toString() : null,
                account.getEmail(),
                account.getName(),
                account.getAddress(),
                account.getEmailVerifiedAt() != null ? account.getEmailVerifiedAt().toString() : null,
                account.getLastLoginAt() != null ? account.getLastLoginAt().toString() : null
        );
    }

    private Optional<StorefrontThemeDocumentDto> readThemeDocumentSetting(String key) {
        Optional<TenantSettingDto> setting = tenantSettingService.findSetting(key);
        if (setting.isEmpty() || setting.get().getValue() == null || setting.get().getValue().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalizeThemeDocument(objectMapper.readValue(setting.get().getValue(), StorefrontThemeDocumentDto.class)));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private StorefrontThemeDocumentDto loadDraftThemeDocument() {
        return readThemeDocumentSetting(DRAFT_THEME_DOCUMENT_KEY)
                .orElseGet(this::buildDefaultThemeDocument);
    }

    private Optional<StorefrontThemeDocumentDto> loadPublishedThemeDocument() {
        return storefrontPublishVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(this::readThemeSnapshot)
                .map(StorefrontThemeSnapshotDto::getThemeDocument);
    }

    private void persistDraftThemeDocument(StorefrontThemeDocumentDto document) {
        writeJsonSetting(DRAFT_THEME_DOCUMENT_KEY, normalizeThemeDocument(document));
        writeStringSetting(THEME_SCHEMA_VERSION_KEY, THEME_SCHEMA_VERSION);
    }

    private String writeThemeSnapshot(StorefrontThemeSnapshotDto snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize storefront snapshot", exception);
        }
    }

    private StorefrontThemeSnapshotDto readThemeSnapshot(StorefrontPublishVersion version) {
        try {
            StorefrontThemeSnapshotDto snapshot = objectMapper.readValue(version.getSnapshotJson(), StorefrontThemeSnapshotDto.class);
            if (snapshot.getThemeDocument() == null) {
                snapshot.setThemeDocument(buildDefaultThemeDocument());
            }
            if (snapshot.getConfig() == null && snapshot.getThemeDocument() != null) {
                snapshot.setConfig(deriveLegacyConfig(snapshot.getThemeDocument()));
            }
            snapshot.setSchemaVersion(snapshot.getSchemaVersion() != null ? snapshot.getSchemaVersion() : THEME_SCHEMA_VERSION);
            return snapshot;
        } catch (JsonProcessingException exception) {
            // Pre-V66 snapshots had a different shape — V66 wipes them, so this branch
            // is dead in practice. Surface a clean fallback instead of crashing.
            StorefrontThemeDocumentDto doc = buildDefaultThemeDocument();
            return new StorefrontThemeSnapshotDto(
                    THEME_SCHEMA_VERSION,
                    version.getId().toString(),
                    doc,
                    deriveLegacyConfig(doc)
            );
        }
    }

    private StorefrontPublishVersionDto mapPublishVersion(StorefrontPublishVersion version) {
        return new StorefrontPublishVersionDto(
                version.getId().toString(),
                version.getVersionNumber(),
                "Publish " + version.getVersionNumber(),
                version.getNote(),
                version.getPublishedAt().toString(),
                version.getRestoredFromVersionNumber(),
                "PUBLISHED",
                version.getThemeKey(),
                version.getThemeVersion()
        );
    }

    private StorefrontThemeDocumentDto normalizeThemeDocument(StorefrontThemeDocumentDto document) {
        if (document == null || document.getTemplates() == null || document.getTemplates().isEmpty()) {
            return buildDefaultThemeDocument();
        }
        StorefrontThemeDocumentDto fallback = buildDefaultThemeDocument();
        StorefrontThemeDocumentDto normalized = new StorefrontThemeDocumentDto();
        normalized.setTemplateKey(document.getTemplateKey() != null ? document.getTemplateKey() : fallback.getTemplateKey());
        normalized.setSchemaVersion(THEME_SCHEMA_VERSION);
        normalized.setSettings(document.getSettings() != null ? new LinkedHashMap<>(document.getSettings()) : new LinkedHashMap<>(fallback.getSettings()));
        normalized.setTemplates(new LinkedHashMap<>(document.getTemplates()));
        normalized.getTemplates().computeIfAbsent("header", ignored -> fallback.getTemplates().get("header"));
        normalized.getTemplates().computeIfAbsent("home", ignored -> fallback.getTemplates().get("home"));
        normalized.getTemplates().computeIfAbsent("footer", ignored -> fallback.getTemplates().get("footer"));
        normalized.getTemplates().values().forEach(this::upgradeTemplateToSectionGroups);
        return normalized;
    }

    /**
     * Construct a fresh theme document from the active manifest's templatePresets.
     * One section per preset entry, with empty settings the admin fills in.
     * Replaces the old migrateLegacyConfigToThemeDocument chain that mutated
     * legacy v1 page structures.
     */
    private StorefrontThemeDocumentDto buildDefaultThemeDocument() {
        StorefrontSiteDto site = defaultSite();
        String templateKey = site.getTemplateKey();
        StorefrontThemeManifestDto manifest = storefrontThemeRegistry.findByKey(templateKey)
                .map(storefrontThemeRegistry::resolveWithInheritance)
                .orElse(null);

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("site", objectMapper.convertValue(site, new TypeReference<Map<String, Object>>() {}));
        settings.put("theme", objectMapper.convertValue(defaultTheme(), new TypeReference<Map<String, Object>>() {}));
        settings.put("banners", objectMapper.convertValue(defaultBanners(), new TypeReference<List<Map<String, Object>>>() {}));

        Map<String, StorefrontThemeTemplateDto> templates = new LinkedHashMap<>();
        templates.put("header", buildTemplateFromPresets("header", "Header", manifest));
        templates.put("home", buildTemplateFromPresets("home", "Home page", manifest));
        templates.put("footer", buildTemplateFromPresets("footer", "Footer", manifest));

        StorefrontThemeDocumentDto doc = new StorefrontThemeDocumentDto(templateKey, THEME_SCHEMA_VERSION, settings, templates);
        doc.getTemplates().values().forEach(this::upgradeTemplateToSectionGroups);
        return doc;
    }

    private StorefrontThemeTemplateDto buildTemplateFromPresets(String templateId, String label, StorefrontThemeManifestDto manifest) {
        StorefrontThemeTemplateDto template = new StorefrontThemeTemplateDto(templateId, label, new ArrayList<>(), new ArrayList<>());
        if (manifest == null || manifest.getTemplatePresets() == null) return template;
        Object presetsObj = manifest.getTemplatePresets().get(templateId);
        if (!(presetsObj instanceof List<?> presets)) return template;
        for (Object presetObj : presets) {
            if (!(presetObj instanceof Map<?, ?> preset)) continue;
            String type = String.valueOf(preset.get("type"));
            Object labelObj = preset.get("label");
            String sectionLabel = labelObj != null ? labelObj.toString() : type;
            String group = lookupSectionGroupFromManifest(manifest, type);
            template.getSections().add(new StorefrontThemeSectionDto(
                    type + "-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    type,
                    sectionLabel,
                    "default",
                    true,
                    group,
                    new LinkedHashMap<>(),
                    new ArrayList<>()
            ));
        }
        return template;
    }

    private String lookupSectionGroupFromManifest(StorefrontThemeManifestDto manifest, String sectionType) {
        if (manifest == null || manifest.getSectionDefinitions() == null || sectionType == null) return SECTION_GROUP_BODY;
        Object def = manifest.getSectionDefinitions().get(sectionType);
        if (!(def instanceof Map<?, ?> defMap)) return SECTION_GROUP_BODY;
        Object group = defMap.get("group");
        return group != null ? group.toString() : SECTION_GROUP_BODY;
    }

    /**
     * Schema v2 upgrader: ensures every template carries a sectionGroups list.
     * v1 templates have only a flat `sections` list — wrap them into a single
     * "body" group so renderers and the admin editor can speak the new shape.
     * Flat `sections` are kept in sync (sectionGroups[*].sections.flatMap) for
     * backward compat with consumers that haven't been migrated to groups yet.
     */
    private void upgradeTemplateToSectionGroups(StorefrontThemeTemplateDto template) {
        if (template == null) return;
        if (template.getSectionGroups() == null) {
            template.setSectionGroups(new ArrayList<>());
        }

        // Collect every section from both representations, then re-bucket by groupType.
        List<StorefrontThemeSectionDto> allSections = new ArrayList<>();
        if (template.getSections() != null) allSections.addAll(template.getSections());
        for (StorefrontThemeSectionGroupDto group : template.getSectionGroups()) {
            if (group.getSections() != null) {
                for (StorefrontThemeSectionDto section : group.getSections()) {
                    if (section.getGroupType() == null || section.getGroupType().isBlank()) {
                        section.setGroupType(group.getType());
                    }
                    if (!containsById(allSections, section)) {
                        allSections.add(section);
                    }
                }
            }
        }

        String templateDefaultGroup = inferDefaultGroupType(template.getId());
        Map<String, List<StorefrontThemeSectionDto>> buckets = new LinkedHashMap<>();
        for (String groupType : List.of(SECTION_GROUP_HEADER, SECTION_GROUP_BODY, SECTION_GROUP_FOOTER, "aside")) {
            buckets.put(groupType, new ArrayList<>());
        }
        for (StorefrontThemeSectionDto section : allSections) {
            String groupType = section.getGroupType();
            if (groupType == null || groupType.isBlank()) {
                groupType = templateDefaultGroup;
                section.setGroupType(groupType);
            }
            buckets.computeIfAbsent(groupType, k -> new ArrayList<>()).add(section);
        }

        List<StorefrontThemeSectionGroupDto> groups = new ArrayList<>();
        for (Map.Entry<String, List<StorefrontThemeSectionDto>> entry : buckets.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            groups.add(new StorefrontThemeSectionGroupDto(entry.getKey(), entry.getKey(), capitalize(entry.getKey()), entry.getValue()));
        }
        template.setSectionGroups(groups);

        // Flat list mirrors groups in declared group order for backward compat.
        List<StorefrontThemeSectionDto> flat = new ArrayList<>();
        for (StorefrontThemeSectionGroupDto group : groups) {
            if (group.getSections() != null) flat.addAll(group.getSections());
        }
        template.setSections(flat);
    }

    private boolean containsById(List<StorefrontThemeSectionDto> list, StorefrontThemeSectionDto candidate) {
        if (candidate == null || candidate.getId() == null) return false;
        for (StorefrontThemeSectionDto s : list) {
            if (candidate.getId().equals(s.getId())) return true;
        }
        return false;
    }

    private String inferDefaultGroupType(String templateId) {
        if (SECTION_GROUP_HEADER.equals(templateId)) return SECTION_GROUP_HEADER;
        if (SECTION_GROUP_FOOTER.equals(templateId)) return SECTION_GROUP_FOOTER;
        return SECTION_GROUP_BODY;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private StorefrontConfigDto deriveLegacyConfig(StorefrontThemeDocumentDto document) {
        StorefrontThemeDocumentDto normalized = normalizeThemeDocument(document);
        Map<String, Object> siteSettings = mapSettingsGroup(normalized.getSettings(), "site");
        Map<String, Object> themeSettings = mapSettingsGroup(normalized.getSettings(), "theme");
        List<StorefrontBannerDto> banners = objectMapper.convertValue(
                normalized.getSettings().getOrDefault("banners", defaultBanners()),
                new TypeReference<List<StorefrontBannerDto>>() {}
        );

        StorefrontSiteDto site = objectMapper.convertValue(siteSettings, StorefrontSiteDto.class);
        StorefrontThemeDto theme = objectMapper.convertValue(themeSettings, StorefrontThemeDto.class);

        site = mergeSite(site);
        theme = mergeTheme(theme);

        StorefrontThemeTemplateDto headerTemplate = normalized.getTemplates().get("header");
        List<StorefrontNavItemDto> navigationItems = deriveNavigationItems(headerTemplate);
        applyHeaderSettings(site, headerTemplate);

        StorefrontThemeTemplateDto homeTemplate = normalized.getTemplates().get("home");
        StorefrontPageDto homePage = new StorefrontPageDto(
                "home",
                "Home page",
                (homeTemplate != null ? homeTemplate.getSections() : List.<StorefrontThemeSectionDto>of()).stream()
                        .map(this::deriveLegacySection)
                        .toList()
        );

        StorefrontThemeTemplateDto footerTemplate = normalized.getTemplates().get("footer");
        applyFooterSettings(site, footerTemplate);

        StorefrontPageDto headerPage = new StorefrontPageDto(
                "header",
                "Header",
                (headerTemplate != null ? headerTemplate.getSections() : List.<StorefrontThemeSectionDto>of()).stream()
                        .map(this::deriveLegacySection)
                        .toList()
        );

        StorefrontPageDto footerPage = new StorefrontPageDto(
                "footer",
                "Footer",
                (footerTemplate != null ? footerTemplate.getSections() : List.<StorefrontThemeSectionDto>of()).stream()
                        .map(this::deriveLegacySection)
                        .toList()
        );

        return enrichWithDomains(new StorefrontConfigDto(
                site,
                theme,
                navigationItems,
                banners,
                headerPage,
                homePage,
                footerPage,
                storefrontDomainService.getDomainContextForCurrentTenant()
        ));
    }

    private Map<String, Object> mapSettingsGroup(Map<String, Object> settings, String key) {
        Object value = settings != null ? settings.get(key) : null;
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private StorefrontSiteDto mergeSite(StorefrontSiteDto site) {
        if (site == null) {
            site = defaultSite();
        }
        StorefrontSiteDto fallback = defaultSite();
        return new StorefrontSiteDto(
                site.isEnabled(),
                site.getTemplateKey() != null ? site.getTemplateKey() : fallback.getTemplateKey(),
                site.getName() != null ? site.getName() : fallback.getName(),
                site.getTagline() != null ? site.getTagline() : fallback.getTagline(),
                site.getAnnouncement() != null ? site.getAnnouncement() : fallback.getAnnouncement(),
                site.getDomain() != null ? site.getDomain() : fallback.getDomain(),
                site.getModulePlan() != null ? site.getModulePlan() : fallback.getModulePlan(),
                site.getLogoUrl() != null ? site.getLogoUrl() : fallback.getLogoUrl(),
                site.getIconUrl() != null ? site.getIconUrl() : fallback.getIconUrl(),
                site.getLogoWidth() != null ? site.getLogoWidth() : fallback.getLogoWidth(),
                site.getProductCardHoverMode() != null ? site.getProductCardHoverMode() : fallback.getProductCardHoverMode(),
                site.getProductCardHoverZoom() != null ? site.getProductCardHoverZoom() : fallback.getProductCardHoverZoom(),
                site.getDrawerButtonLabel() != null ? site.getDrawerButtonLabel() : fallback.getDrawerButtonLabel(),
                site.getDrawerLabel() != null ? site.getDrawerLabel() : fallback.getDrawerLabel(),
                site.getDrawerTitle() != null ? site.getDrawerTitle() : fallback.getDrawerTitle(),
                site.getDrawerDescription() != null ? site.getDrawerDescription() : fallback.getDrawerDescription(),
                site.getAllCollectionsLabel() != null ? site.getAllCollectionsLabel() : fallback.getAllCollectionsLabel(),
                site.getSearchPlaceholder() != null ? site.getSearchPlaceholder() : fallback.getSearchPlaceholder(),
                site.getCartLabel() != null ? site.getCartLabel() : fallback.getCartLabel(),
                site.getWarehouseId() != null ? site.getWarehouseId() : fallback.getWarehouseId(),
                site.getFooterCatalogLabel() != null ? site.getFooterCatalogLabel() : fallback.getFooterCatalogLabel(),
                site.getFooterCollectionsLabel() != null ? site.getFooterCollectionsLabel() : fallback.getFooterCollectionsLabel(),
                site.getFooterTrackingLabel() != null ? site.getFooterTrackingLabel() : fallback.getFooterTrackingLabel(),
                site.getPublishStatus() != null ? site.getPublishStatus() : fallback.getPublishStatus(),
                site.getPublishedVersion(),
                site.getLastPublishedAt()
        );
    }

    private StorefrontConfigDto enrichWithDomains(StorefrontConfigDto config) {
        if (config == null) {
            return null;
        }

        StorefrontDomainContextDto domains = storefrontDomainService.getDomainContextForCurrentTenant();
        config.setDomains(domains);
        if (config.getSite() != null && domains != null) {
            config.getSite().setDomain(domains.getPrimaryHostname() != null ? domains.getPrimaryHostname() : domains.getPlatformFallbackHost());
        }
        return config;
    }

    private StorefrontThemeDto mergeTheme(StorefrontThemeDto theme) {
        StorefrontThemeDto fallback = defaultTheme();
        return new StorefrontThemeDto(
                theme.getPrimary() != null ? theme.getPrimary() : fallback.getPrimary(),
                theme.getSecondary() != null ? theme.getSecondary() : fallback.getSecondary(),
                theme.getAccent() != null ? theme.getAccent() : fallback.getAccent(),
                theme.getSurface() != null ? theme.getSurface() : fallback.getSurface(),
                theme.getText() != null ? theme.getText() : fallback.getText(),
                theme.getRadius() != null ? theme.getRadius() : fallback.getRadius(),
                theme.getHeroAlignment() != null ? theme.getHeroAlignment() : fallback.getHeroAlignment(),
                theme.getHeadingFont() != null ? theme.getHeadingFont() : fallback.getHeadingFont(),
                theme.getBodyFont() != null ? theme.getBodyFont() : fallback.getBodyFont()
        );
    }

    private List<StorefrontNavItemDto> deriveNavigationItems(StorefrontThemeTemplateDto headerTemplate) {
        if (headerTemplate == null) {
            return defaultNavigation();
        }
        return headerTemplate.getSections().stream()
                .filter(section -> "header_nav".equals(section.getType()))
                .findFirst()
                .map(section -> section.getBlocks().stream()
                        .filter(block -> Boolean.TRUE.equals(block.getEnabled()) && "nav_link".equals(block.getType()))
                        .map(block -> new StorefrontNavItemDto(
                                block.getId(),
                                String.valueOf(block.getSettings().getOrDefault("label", block.getLabel())),
                                String.valueOf(block.getSettings().getOrDefault("href", "/"))
                        ))
                        .toList())
                .filter(items -> !items.isEmpty())
                .orElseGet(this::defaultNavigation);
    }

    private void applyHeaderSettings(StorefrontSiteDto site, StorefrontThemeTemplateDto headerTemplate) {
        if (headerTemplate == null) {
            return;
        }
        headerTemplate.getSections().forEach(section -> {
            if ("announcement_bar".equals(section.getType())) {
                site.setAnnouncement(String.valueOf(section.getSettings().getOrDefault("text", site.getAnnouncement())));
            }
            if ("header_nav".equals(section.getType())) {
                site.setDrawerButtonLabel(String.valueOf(section.getSettings().getOrDefault("drawerButtonLabel", site.getDrawerButtonLabel())));
                site.setDrawerLabel(String.valueOf(section.getSettings().getOrDefault("drawerLabel", site.getDrawerLabel())));
                site.setDrawerTitle(String.valueOf(section.getSettings().getOrDefault("drawerTitle", site.getDrawerTitle())));
                site.setDrawerDescription(String.valueOf(section.getSettings().getOrDefault("drawerDescription", site.getDrawerDescription())));
                site.setAllCollectionsLabel(String.valueOf(section.getSettings().getOrDefault("allCollectionsLabel", site.getAllCollectionsLabel())));
                site.setSearchPlaceholder(String.valueOf(section.getSettings().getOrDefault("searchPlaceholder", site.getSearchPlaceholder())));
                site.setCartLabel(String.valueOf(section.getSettings().getOrDefault("cartLabel", site.getCartLabel())));
            }
        });
    }

    private void applyFooterSettings(StorefrontSiteDto site, StorefrontThemeTemplateDto footerTemplate) {
        if (footerTemplate == null) {
            return;
        }
        footerTemplate.getSections().stream()
                .filter(section -> "footer_links".equals(section.getType()))
                .findFirst()
                .ifPresent(section -> {
                    List<StorefrontThemeBlockDto> blocks = section.getBlocks() != null ? section.getBlocks() : List.of();
                    if (blocks.size() > 0) {
                        site.setFooterCatalogLabel(String.valueOf(blocks.get(0).getSettings().getOrDefault("label", site.getFooterCatalogLabel())));
                    }
                    if (blocks.size() > 1) {
                        site.setFooterCollectionsLabel(String.valueOf(blocks.get(1).getSettings().getOrDefault("label", site.getFooterCollectionsLabel())));
                    }
                    if (blocks.size() > 2) {
                        site.setFooterTrackingLabel(String.valueOf(blocks.get(2).getSettings().getOrDefault("label", site.getFooterTrackingLabel())));
                    }
                });
    }

    private StorefrontSectionDto deriveLegacySection(StorefrontThemeSectionDto section) {
        Map<String, Object> config = section.getSettings() != null ? new LinkedHashMap<>(section.getSettings()) : new LinkedHashMap<>();

        if ("announcement_bar".equals(section.getType())) {
            config.put("text", section.getSettings().getOrDefault("text", ""));
            config.put("enabled", section.getSettings().getOrDefault("enabled", Boolean.TRUE.equals(section.getEnabled())));
        } else if ("header_nav".equals(section.getType())) {
            config.put("navBlockIds", (section.getBlocks() != null ? section.getBlocks() : List.<StorefrontThemeBlockDto>of()).stream()
                    .filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                    .map(StorefrontThemeBlockDto::getId)
                    .toList());
        } else if ("hero_banner".equals(section.getType())) {
            List<StorefrontThemeBlockDto> blocks = section.getBlocks() != null ? section.getBlocks() : List.of();
            if (blocks.size() > 0) {
                config.put("ctaLabel", blocks.get(0).getSettings().getOrDefault("label", ""));
                config.put("ctaHref", blocks.get(0).getSettings().getOrDefault("href", ""));
            }
            if (blocks.size() > 1) {
                config.put("secondaryCtaLabel", blocks.get(1).getSettings().getOrDefault("label", ""));
                config.put("secondaryCtaHref", blocks.get(1).getSettings().getOrDefault("href", ""));
            }
        } else if ("promo_strip".equals(section.getType())) {
            config.put("items", (section.getBlocks() != null ? section.getBlocks() : List.<StorefrontThemeBlockDto>of()).stream()
                    .filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                    .map(block -> String.valueOf(block.getSettings().getOrDefault("text", "")))
                    .toList());
        } else if ("featured_products".equals(section.getType())) {
            config.put("productSlugs", (section.getBlocks() != null ? section.getBlocks() : List.<StorefrontThemeBlockDto>of()).stream()
                    .filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                    .map(block -> String.valueOf(block.getSettings().getOrDefault("productSlug", "")))
                    .filter(value -> !value.isBlank())
                    .toList());
        } else if ("featured_collections".equals(section.getType())) {
            config.put("collectionSlugs", (section.getBlocks() != null ? section.getBlocks() : List.<StorefrontThemeBlockDto>of()).stream()
                    .filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                    .map(block -> String.valueOf(block.getSettings().getOrDefault("collectionSlug", "")))
                    .filter(value -> !value.isBlank())
                    .toList());
        } else if ("footer_links".equals(section.getType())) {
            config.put("linkBlockIds", (section.getBlocks() != null ? section.getBlocks() : List.<StorefrontThemeBlockDto>of()).stream()
                    .filter(block -> Boolean.TRUE.equals(block.getEnabled()))
                    .map(StorefrontThemeBlockDto::getId)
                    .toList());
        }

        return new StorefrontSectionDto(
                section.getId(),
                section.getLabel(),
                section.getType(),
                Boolean.TRUE.equals(section.getEnabled()),
                section.getVariant(),
                config,
                section.getBlocks() != null ? new ArrayList<>(section.getBlocks()) : new ArrayList<>()
        );
    }

    /**
     * Overlay the manifest's default values for the given group key onto the
     * draft. Existing user-set values WIN — defaults only fill in fields the
     * tenant hasn't explicitly set, so switching themes doesn't wipe the
     * tenant's brand name / domain / SEO config.
     */
    private void mergeSettingsGroup(Map<String, Object> draftSettings, Map<String, Object> manifestDefaults, String groupKey) {
        Object defaultsObj = manifestDefaults != null ? manifestDefaults.get(groupKey) : null;
        if (!(defaultsObj instanceof Map<?, ?> defaultsMap)) return;
        Object existingObj = draftSettings.get(groupKey);
        Map<String, Object> existing;
        if (existingObj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) existingObj;
            existing = cast;
        } else {
            existing = new LinkedHashMap<>();
            draftSettings.put(groupKey, existing);
        }
        for (Map.Entry<?, ?> entry : defaultsMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (!existing.containsKey(key) || existing.get(key) == null || "".equals(existing.get(key))) {
                existing.put(key, entry.getValue());
            }
        }
    }

    private void stampThemeMetadata(StorefrontPublishVersion version, StorefrontThemeDocumentDto themeDocument) {
        String themeKey = themeDocument != null ? themeDocument.getTemplateKey() : null;
        version.setThemeKey(themeKey);
        version.setThemeVersion(storefrontThemeRegistry.findByKey(themeKey)
                .map(StorefrontThemeManifestDto::getVersion)
                .orElse(null));
    }

    private Map<String, Object> buildThemeSchema(String templateKey) {
        Optional<Map<String, Object>> fromRegistry = storefrontThemeRegistry.buildSchemaForKey(templateKey, THEME_SCHEMA_VERSION);
        if (fromRegistry.isPresent()) {
            return fromRegistry.get();
        }
        // Fallback: inline definition (kept for safety if no manifest is registered for this templateKey)
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("version", THEME_SCHEMA_VERSION);
        schema.put("templateKey", templateKey);
        schema.put("templateTree", List.of(
                Map.of("id", "settings", "label", "Theme settings"),
                Map.of("id", "header", "label", "Header"),
                Map.of("id", "home", "label", "Home page"),
                Map.of("id", "footer", "label", "Footer")
        ));
        schema.put("settingsGroups", List.of(
                Map.of(
                        "id", "site",
                        "label", "Brand & storefront",
                        "fields", List.of(
                                field("templateKey", "Template", "select", List.of(option("boutique", "Boutique"), option("minimal", "Minimal"), option("magazine", "Magazine"))),
                                field("name", "Store name", "text"),
                                field("tagline", "Tagline", "text"),
                                field("announcement", "Announcement", "textarea"),
                                field("domain", "Domain", "text"),
                                field("logoUrl", "Logo URL", "text"),
                                field("iconUrl", "Icon URL", "text"),
                                field("logoWidth", "Logo width", "number"),
                                field("productCardHoverMode", "Product hover image", "select", List.of(option("NONE", "None"), option("SECOND_IMAGE", "Second image"))),
                                field("productCardHoverZoom", "Hover zoom", "toggle"),
                                field("warehouseId", "Storefront warehouse", "entity_warehouse"),
                                field("shippingFlatRate", "Shipping flat rate", "number"),
                                field("freeShippingThreshold", "Free shipping threshold", "number"),
                                field("taxRate", "Tax rate (%)", "number")
                        )
                ),
                Map.of(
                        "id", "seo",
                        "label", "SEO & preferences",
                        "fields", List.of(
                                field("seoTitle", "Default page title", "text"),
                                field("seoDescription", "Default meta description", "textarea"),
                                field("faviconUrl", "Favicon URL", "text")
                        )
                ),
                Map.of(
                        "id", "social",
                        "label", "Social media",
                        "fields", List.of(
                                field("socialFacebook", "Facebook URL", "text"),
                                field("socialInstagram", "Instagram URL", "text"),
                                field("socialTwitter", "X (Twitter) URL", "text"),
                                field("socialTiktok", "TikTok URL", "text"),
                                field("socialYoutube", "YouTube URL", "text")
                        )
                ),
                Map.of(
                        "id", "checkout",
                        "label", "Checkout",
                        "fields", List.of(
                                field("checkoutRequirePhone", "Require phone number", "toggle"),
                                field("checkoutAllowNotes", "Allow order notes", "toggle"),
                                field("checkoutTermsText", "Terms & conditions text", "textarea"),
                                field("emailOrderConfirmation", "Send order confirmation email", "toggle")
                        )
                ),
                Map.of(
                        "id", "theme",
                        "label", "Theme tokens",
                        "fields", List.of(
                                field("primary", "Primary", "color"),
                                field("secondary", "Secondary", "color"),
                                field("accent", "Accent", "color"),
                                field("surface", "Surface", "color"),
                                field("text", "Text", "color"),
                                field("radius", "Radius", "number"),
                                field("headingFont", "Heading font", "text"),
                                field("bodyFont", "Body font", "text")
                        )
                )
        ));
        schema.put("sectionDefinitions", new LinkedHashMap<>(Map.of(
                "announcement_bar", Map.of("label", "Announcement bar", "settings", List.of(field("text", "Text", "textarea"), field("enabled", "Enabled", "toggle")), "blockTypes", List.of()),
                "header_nav", Map.of("label", "Header navigation", "settings", List.of(
                        field("drawerButtonLabel", "Drawer button label", "text"),
                        field("drawerLabel", "Drawer eyebrow", "text"),
                        field("drawerTitle", "Drawer title", "text"),
                        field("drawerDescription", "Drawer description", "textarea"),
                        field("allCollectionsLabel", "All collections label", "text"),
                        field("searchPlaceholder", "Search placeholder", "text"),
                        field("cartLabel", "Cart label", "text")
                ), "blockTypes", List.of("nav_link")),
                "hero_banner", Map.of("label", "Hero banner", "settings", List.of(
                        field("eyebrow", "Eyebrow", "text"),
                        field("headline", "Headline", "text"),
                        field("subheadline", "Subheadline", "textarea"),
                        field("imageUrl", "Image URL", "text")
                ), "blockTypes", List.of("hero_button")),
                "promo_strip", Map.of("label", "Promo strip", "settings", List.of(), "blockTypes", List.of("promo_item")),
                "featured_products", Map.of("label", "Featured products", "settings", List.of(
                        field("source", "Source", "select", List.of(option("manual", "Manual selection"), option("newest", "Newest products"), option("featured", "Featured products"), option("collection", "By collection"))),
                        field("collectionSlug", "Collection source", "entity_collection"),
                        field("limit", "Limit", "number"),
                        field("eyebrow", "Eyebrow", "text"),
                        field("title", "Title", "text"),
                        field("description", "Description", "textarea"),
                        field("ctaLabel", "CTA label", "text")
                ), "blockTypes", List.of("product_reference")),
                "featured_collections", Map.of("label", "Featured collections", "settings", List.of(
                        field("limit", "Limit", "number"),
                        field("eyebrow", "Eyebrow", "text"),
                        field("title", "Title", "text"),
                        field("description", "Description", "textarea"),
                        field("ctaLabel", "CTA label", "text"),
                        field("collectionKickerLabel", "Card kicker", "text"),
                        field("collectionCardCtaLabel", "Card CTA", "text")
                ), "blockTypes", List.of("collection_reference")),
                "footer_links", Map.of("label", "Footer links", "settings", List.of(field("brandName", "Brand name", "text"), field("tagline", "Tagline", "textarea")), "blockTypes", List.of("footer_link"))
        )));
        schema.put("blockDefinitions", new LinkedHashMap<>(Map.of(
                "nav_link", Map.of("label", "Navigation link", "settings", List.of(field("label", "Label", "text"), field("href", "Link", "text"))),
                "hero_button", Map.of("label", "Hero button", "settings", List.of(field("label", "Label", "text"), field("href", "Link", "text"))),
                "promo_item", Map.of("label", "Promo item", "settings", List.of(field("text", "Text", "text"))),
                "product_reference", Map.of("label", "Product reference", "settings", List.of(field("productSlug", "Product", "entity_product"))),
                "collection_reference", Map.of("label", "Collection reference", "settings", List.of(field("collectionSlug", "Collection", "entity_collection"))),
                "footer_link", Map.of("label", "Footer link", "settings", List.of(field("label", "Label", "text"), field("href", "Link", "text")))
        )));
        schema.put("templatePresets", new LinkedHashMap<>(Map.of(
                "header", List.of(Map.of("type", "announcement_bar", "label", "Announcement bar"), Map.of("type", "header_nav", "label", "Header navigation")),
                "home", List.of(Map.of("type", "hero_banner", "label", "Hero banner"), Map.of("type", "promo_strip", "label", "Promo strip"), Map.of("type", "featured_products", "label", "Featured products"), Map.of("type", "featured_collections", "label", "Featured collections")),
                "footer", List.of(Map.of("type", "footer_links", "label", "Footer links"))
        )));
        return schema;
    }

    private Map<String, Object> field(String id, String label, String type) {
        return field(id, label, type, List.of());
    }

    private Map<String, Object> field(String id, String label, String type, List<Map<String, Object>> options) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("id", id);
        field.put("label", label);
        field.put("type", type);
        if (options != null && !options.isEmpty()) {
            field.put("options", options);
        }
        return field;
    }

    private Map<String, Object> option(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    private StorefrontProductDto mapProduct(ProductVariant variant,
                                            Map<UUID, List<ProductVariant>> siblingsByTemplate,
                                            Warehouse storefrontWarehouse,
                                            boolean useTemplateSlugAsPrimarySlug) {
        List<ProductVariant> siblingVariants = siblingsByTemplate.getOrDefault(variant.getTemplate().getId(), List.of(variant));
        String name = variant.getTemplate().getStorefrontTitle() != null && !variant.getTemplate().getStorefrontTitle().isBlank()
                ? variant.getTemplate().getStorefrontTitle()
                : variant.getTemplate().getName();
        String templateSlug = buildTemplateProductSlug(variant);
        String slug = useTemplateSlugAsPrimarySlug ? templateSlug : buildProductSlug(variant, siblingVariants);
        String summary = variant.getTemplate().getStorefrontDescription() != null && !variant.getTemplate().getStorefrontDescription().isBlank()
                ? variant.getTemplate().getStorefrontDescription()
                : variant.getTemplate().getDescription();
        String imageUrl = variant.getTemplate().getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .findFirst()
                .or(() -> variant.getTemplate().getImages().stream().findFirst())
                .map(this::resolveStorefrontImageUrl)
                .orElse(null);
        List<String> imageUrls = variant.getTemplate().getImages().stream()
                .map(this::resolveStorefrontImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
        BigDecimal availableToPromise = resolveAvailableToPromise(variant, storefrontWarehouse);
        String availabilityLabel = toAvailabilityLabel(availableToPromise);

        StorefrontProductDto dto = new StorefrontProductDto();
        dto.setProductTemplateId(variant.getTemplate().getId().toString());
        dto.setProductSlug(templateSlug);
        dto.setProductVariantId(variant.getId().toString());
        dto.setSlug(slug);
        dto.setSku(variant.getSku());
        dto.setName(name);
        dto.setCategory(variant.getTemplate().getCategory() != null ? variant.getTemplate().getCategory().getName() : "Uncategorized");
        dto.setCollectionSlug(variant.getTemplate().getCategory() != null
                ? (variant.getTemplate().getCategory().getStorefrontSlug() != null && !variant.getTemplate().getCategory().getStorefrontSlug().isBlank()
                    ? variant.getTemplate().getCategory().getStorefrontSlug()
                    : slugify(variant.getTemplate().getCategory().getName()))
                : null);
        dto.setCollectionTitle(variant.getTemplate().getCategory() != null
                ? (variant.getTemplate().getCategory().getStorefrontTitle() != null && !variant.getTemplate().getCategory().getStorefrontTitle().isBlank()
                    ? variant.getTemplate().getCategory().getStorefrontTitle()
                    : variant.getTemplate().getCategory().getName())
                : null);
        dto.setParentCollectionSlug(resolveParentCollectionSlug(variant.getTemplate().getCategory()));
        dto.setParentCollectionTitle(resolveParentCollectionTitle(variant.getTemplate().getCategory()));
        dto.setCategoryPath(buildCategoryPath(variant.getTemplate().getCategory()));
        dto.setCollectionTrailSlugs(buildCategoryTrailSlugs(variant.getTemplate().getCategory()));
        dto.setCollectionTrailTitles(buildCategoryTrailTitles(variant.getTemplate().getCategory()));
        dto.setVariantLabel(buildVariantLabel(variant));
        dto.setPrice(variant.getPrice());
        dto.setCompareAtPrice(variant.getCompareAtPrice());
        dto.setCurrency("BDT");
        dto.setSummary(summary != null && !summary.isBlank() ? summary : "Inventory-backed catalog item ready for storefront publishing.");
        dto.setImageUrl(imageUrl);
        dto.setImageUrls(imageUrls);
        dto.setSeoTitle(variant.getTemplate().getStorefrontSeoTitle());
        dto.setSeoDescription(variant.getTemplate().getStorefrontSeoDescription());
        dto.setUomName(variant.getTemplate().getUom() != null ? variant.getTemplate().getUom().getName() : null);
        dto.setBadge(variant.getStorefrontBadge());
        dto.setFeatured(Boolean.TRUE.equals(variant.getStorefrontFeatured()));
        dto.setAvailableToPromise(availableToPromise);
        dto.setAvailabilityLabel(availabilityLabel);
        dto.setVariants(mapVariantOptions(siblingVariants, siblingsByTemplate, storefrontWarehouse));
        return dto;
    }

    private ProductVariant resolvePrimaryVariant(List<ProductVariant> siblingVariants) {
        if (siblingVariants == null || siblingVariants.isEmpty()) {
            return null;
        }

        return siblingVariants.stream()
                .sorted(Comparator
                        .comparing(ProductVariant::getStorefrontFeatured, Comparator.nullsLast(Boolean::compareTo)).reversed()
                        .thenComparing(ProductVariant::getPrice, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(ProductVariant::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .findFirst()
                .orElse(siblingVariants.get(0));
    }

    private boolean matchesQuery(StorefrontProductDto product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase();
        return String.join(" ",
                        product.getName() != null ? product.getName() : "",
                        product.getSummary() != null ? product.getSummary() : "",
                        product.getCategory() != null ? product.getCategory() : "",
                        product.getCollectionTitle() != null ? product.getCollectionTitle() : "",
                        product.getSku() != null ? product.getSku() : "")
                .toLowerCase()
                .contains(needle);
    }

    private boolean matchesCollection(StorefrontProductDto product, String collectionSlug) {
        if (collectionSlug == null || collectionSlug.isBlank()) {
            return true;
        }
        String normalizedCollectionSlug = collectionSlug.trim();
        if (normalizedCollectionSlug.equalsIgnoreCase(product.getCollectionSlug())) {
            return true;
        }
        if (normalizedCollectionSlug.equalsIgnoreCase(product.getParentCollectionSlug())) {
            return true;
        }
        return product.getCollectionTrailSlugs() != null
                && product.getCollectionTrailSlugs().stream().anyMatch(normalizedCollectionSlug::equalsIgnoreCase);
    }

    private Comparator<StorefrontProductDto> productComparator(String sort) {
        String sortKey = sort == null ? "featured" : sort.trim().toLowerCase();
        return switch (sortKey) {
            case "name" -> Comparator.comparing(StorefrontProductDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "price-asc" -> Comparator.comparing(StorefrontProductDto::getPrice, Comparator.nullsLast(BigDecimal::compareTo));
            case "price-desc" -> Comparator.comparing(StorefrontProductDto::getPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
            default -> Comparator
                    .comparing(StorefrontProductDto::getFeatured, Comparator.nullsLast(Boolean::compareTo)).reversed()
                    .thenComparing(StorefrontProductDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

        private StorefrontCartDto toCartDto(Warehouse warehouse, PricingEvaluation pricingEvaluation, String currency) {
        List<StorefrontCartLineDto> lines = pricingEvaluation.getLines().stream()
                .map(line -> mapCartLine(warehouse, line))
                .toList();

        BigDecimal shippingAmount = computeShippingAmount(pricingEvaluation.getNetSubtotal());
        BigDecimal taxAmount = computeTaxAmount(pricingEvaluation.getNetSubtotal());
        BigDecimal beforeGift = pricingEvaluation.getNetSubtotal().add(shippingAmount).add(taxAmount);
        BigDecimal giftAmount = pricingEvaluation.getGiftCardAmount() != null
                ? pricingEvaluation.getGiftCardAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = beforeGift.subtract(giftAmount);
        if (grandTotal.signum() < 0) grandTotal = BigDecimal.ZERO;

        return new StorefrontCartDto(
            currency != null && !currency.isBlank() ? currency : "BDT",
                warehouse.getId().toString(),
                warehouse.getName(),
                pricingEvaluation.getBaseSubtotal(),
                pricingEvaluation.getTotalDiscount(),
                shippingAmount,
                taxAmount,
                grandTotal,
                new ArrayList<>(pricingEvaluation.getAppliedCouponCodes()),
                giftAmount,
                new ArrayList<>(pricingEvaluation.getAppliedGiftCardCodes()),
                lines
        );
    }

    private StorefrontCartLineDto mapCartLine(Warehouse warehouse, PricingEvaluationLine line) {
        ProductVariant variant = line.getProductVariant();
        String name = variant.getTemplate().getStorefrontTitle() != null && !variant.getTemplate().getStorefrontTitle().isBlank()
                ? variant.getTemplate().getStorefrontTitle()
                : variant.getTemplate().getName();
        String slug = variant.getTemplate().getStorefrontSlug() != null && !variant.getTemplate().getStorefrontSlug().isBlank()
                ? variant.getTemplate().getStorefrontSlug()
                : slugify(name + "-" + variant.getSku());
        String imageUrl = variant.getTemplate().getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .findFirst()
                .or(() -> variant.getTemplate().getImages().stream().findFirst())
                .map(image -> image.getUrl())
                .orElse(null);

        return new StorefrontCartLineDto(
            variant.getId(),
                slug,
                variant.getSku(),
                name,
                line.getQuantity(),
                line.getBaseUnitPrice(),
                line.getFinalUnitPrice(),
                line.getLineDiscountAmount(),
                line.getLineTotalAmount(),
                stockReservationService.getAvailableToPromise(variant.getId(), warehouse.getId()),
                imageUrl
        );
    }

    private SalesOrderRequest toSalesOrderRequest(List<StorefrontCartItemRequest> items,
                                                  UUID warehouseId,
                                                  String currency,
                                                  List<String> couponCodes,
                                                  Customer customer,
                                                  LocalDate expectedDeliveryDate) {
        return toSalesOrderRequest(items, warehouseId, currency, couponCodes, List.of(), null, customer, expectedDeliveryDate);
    }

    private SalesOrderRequest toSalesOrderRequest(List<StorefrontCartItemRequest> items,
                                                  UUID warehouseId,
                                                  String currency,
                                                  List<String> couponCodes,
                                                  List<String> giftCardCodes,
                                                  String referralCode,
                                                  Customer customer,
                                                  LocalDate expectedDeliveryDate) {
        SalesOrderRequest request = new SalesOrderRequest();
        request.setCustomerId(customer != null ? customer.getId() : null);
        request.setWarehouseId(warehouseId);
        request.setCurrency(currency != null && !currency.isBlank() ? currency : "BDT");
        request.setCouponCodes(couponCodes != null ? couponCodes : List.of());
        request.setGiftCardCodes(giftCardCodes != null ? giftCardCodes : List.of());
        request.setReferralCode(referralCode);
        request.setPriority(OrderPriority.HIGH);
        request.setExpectedDeliveryDate(expectedDeliveryDate != null ? expectedDeliveryDate : LocalDate.now().plusDays(3));
        request.setItems(items.stream().map(this::toSalesOrderItemRequest).toList());
        return request;
    }

    private SalesOrderItemRequest toSalesOrderItemRequest(StorefrontCartItemRequest item) {
        SalesOrderItemRequest request = new SalesOrderItemRequest();
        request.setProductVariantId(item.getProductVariantId());
        request.setQuantity(item.getQuantity());
        request.setUnitPrice(null);
        return request;
    }

    private Warehouse resolveWarehouse(UUID requestedWarehouseId) {
        if (requestedWarehouseId != null) {
            return warehouseRepository.findById(requestedWarehouseId)
                    .orElseThrow(() -> new BadRequestException("Warehouse not found with ID: " + requestedWarehouseId));
        }

        UUID configuredWarehouseId = readConfiguredStorefrontWarehouseId();
        if (configuredWarehouseId != null) {
            return warehouseRepository.findById(configuredWarehouseId)
                    .orElseThrow(() -> new BadRequestException("Configured storefront warehouse not found with ID: " + configuredWarehouseId));
        }

        return warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("At least one warehouse is required before using storefront checkout"));
    }

    private Warehouse resolveStorefrontWarehouse() {
        UUID configuredWarehouseId = readConfiguredStorefrontWarehouseId();
        if (configuredWarehouseId != null) {
            return warehouseRepository.findById(configuredWarehouseId).orElse(null);
        }
        return warehouseRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .findFirst()
                .orElse(null);
    }

    private Customer resolvePricingCustomer(UUID customerId, String email, String phoneNumber) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new BadRequestException("Customer not found with ID: " + customerId));
        }
        Optional<Customer> emailMatch = Optional.empty();
        Optional<Customer> phoneMatch = Optional.empty();
        if (email != null && !email.isBlank()) {
            emailMatch = customerRepository.findFirstByEmailIgnoreCase(email.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            phoneMatch = customerRepository.findFirstByPhoneNumber(phoneNumber.trim());
        }
        if (emailMatch.isPresent() && phoneMatch.isPresent() && !emailMatch.get().getId().equals(phoneMatch.get().getId())) {
            throw new BadRequestException("Customer email and phone number resolve to different customer records");
        }
        if (emailMatch.isPresent()) {
            return emailMatch.get();
        }
        if (phoneMatch.isPresent()) {
            return phoneMatch.get();
        }

        Customer guest = new Customer();
        guest.setName("Guest Storefront Customer");
        guest.setEmail(email);
        guest.setPhoneNumber(phoneNumber);
        guest.setCategory(CustomerCategory.OTHER);
        guest.setIsActive(true);
        guest.setStatus(CustomerStatus.ACTIVE);
        return guest;
    }

    private UUID readConfiguredStorefrontWarehouseId() {
        try {
            StorefrontSiteDto site = readSetting(SITE_KEY, new TypeReference<>() {}, defaultSite());
            if (site == null || site.getWarehouseId() == null || site.getWarehouseId().isBlank()) {
                return null;
            }
            return UUID.fromString(site.getWarehouseId().trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Configured storefront warehouse is not a valid UUID");
        }
    }

    private List<ProductVariant> loadPublishedStorefrontVariants() {
        return productVariantRepository.findByTemplatePublishedToStorefrontTrueAndTemplateIsActiveTrue().stream()
                .sorted((left, right) -> {
                    Integer leftSort = left.getTemplate().getStorefrontSortOrder() != null ? left.getTemplate().getStorefrontSortOrder() : Integer.MAX_VALUE;
                    Integer rightSort = right.getTemplate().getStorefrontSortOrder() != null ? right.getTemplate().getStorefrontSortOrder() : Integer.MAX_VALUE;
                    int bySort = leftSort.compareTo(rightSort);
                    if (bySort != 0) {
                        return bySort;
                    }
                    return right.getCreatedAt().compareTo(left.getCreatedAt());
                })
                .toList();
    }

    private Map<UUID, List<ProductVariant>> buildSiblingsByTemplate(List<ProductVariant> variants) {
        return variants.stream().collect(Collectors.groupingBy(
                variant -> variant.getTemplate().getId(),
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private String buildTemplateProductSlug(ProductVariant variant) {
        return variant.getTemplate().getStorefrontSlug() != null && !variant.getTemplate().getStorefrontSlug().isBlank()
                ? variant.getTemplate().getStorefrontSlug()
                : slugify((variant.getTemplate().getStorefrontTitle() != null && !variant.getTemplate().getStorefrontTitle().isBlank()
                ? variant.getTemplate().getStorefrontTitle()
                : variant.getTemplate().getName()));
    }

    private String buildProductSlug(ProductVariant variant, List<ProductVariant> siblings) {
        String templateSlug = buildTemplateProductSlug(variant);
        if (siblings.size() <= 1) {
            return templateSlug;
        }
        return slugify(templateSlug + "-" + variant.getSku());
    }

    private String buildVariantLabel(ProductVariant variant) {
        List<String> attributes = variant.getAttributeValues().stream()
                .map(value -> {
                    String attributeName = value.getAttribute() != null ? value.getAttribute().getName() : null;
                    if (attributeName != null && !attributeName.isBlank()) {
                        return attributeName + ": " + value.getValue();
                    }
                    return value.getValue();
                })
                .filter(value -> value != null && !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        if (!attributes.isEmpty()) {
            return String.join(" / ", attributes);
        }
        return variant.getSku();
    }

    private List<StorefrontVariantOptionDto> mapVariantOptions(List<ProductVariant> siblingVariants,
                                                               Map<UUID, List<ProductVariant>> siblingsByTemplate,
                                                               Warehouse storefrontWarehouse) {
        return siblingVariants.stream()
                .map(sibling -> {
                    BigDecimal availableToPromise = resolveAvailableToPromise(sibling, storefrontWarehouse);
                    return new StorefrontVariantOptionDto(
                            sibling.getId().toString(),
                            buildProductSlug(sibling, siblingsByTemplate.getOrDefault(sibling.getTemplate().getId(), List.of(sibling))),
                            sibling.getSku(),
                            buildVariantLabel(sibling),
                            sibling.getPrice(),
                            sibling.getCompareAtPrice(),
                            availableToPromise,
                            toAvailabilityLabel(availableToPromise),
                            Boolean.TRUE.equals(sibling.getStorefrontFeatured()),
                            sibling.getStorefrontBadge()
                    );
                })
                .toList();
    }

    private BigDecimal resolveAvailableToPromise(ProductVariant variant, Warehouse storefrontWarehouse) {
        if (storefrontWarehouse == null) {
            return BigDecimal.ZERO;
        }
        return stockReservationService.getAvailableToPromise(variant.getId(), storefrontWarehouse.getId());
    }

    private String toAvailabilityLabel(BigDecimal availableToPromise) {
        if (availableToPromise == null || availableToPromise.compareTo(BigDecimal.ZERO) <= 0) {
            return "Out of stock";
        }
        if (availableToPromise.compareTo(new BigDecimal("5")) <= 0) {
            return "Low stock";
        }
        return "In stock";
    }

    private Customer resolveOrCreateCheckoutCustomer(StorefrontCheckoutRequest request) {
        Customer customer = resolvePricingCustomer(request.getCustomerId(), request.getCustomerEmail(), request.getCustomerPhoneNumber());
        if (customer.getId() == null) {
            if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
                customer.setName(request.getCustomerName().trim());
            }
            if (request.getContactName() != null && !request.getContactName().isBlank()) {
                customer.setContactName(request.getContactName().trim());
            }
            if (request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
                customer.setAddress(request.getShippingAddress().trim());
            }
            return customerRepository.save(customer);
        }

        return customer;
    }

    private void validateCustomerEligibility(Customer customer) {
        if (customer.getIsActive() != null && !customer.getIsActive()) {
            throw new BadRequestException("Customer is inactive and cannot be used for storefront checkout");
        }
        if (customer.getStatus() == CustomerStatus.BLOCKED || customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BadRequestException("Customer status does not allow storefront checkout: " + customer.getStatus());
        }
    }

    private Map<UUID, ProductVariant> loadVariants(List<StorefrontCartItemRequest> items) {
        Map<UUID, ProductVariant> variants = productVariantRepository.findAllById(
                        items.stream().map(StorefrontCartItemRequest::getProductVariantId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        if (variants.size() != items.stream().map(StorefrontCartItemRequest::getProductVariantId).collect(Collectors.toSet()).size()) {
            throw new BadRequestException("One or more storefront cart items reference missing product variants");
        }
        return variants;
    }

    private Map<UUID, Deque<PricingEvaluationLine>> buildLineQueues(PricingEvaluation pricingEvaluation) {
        Map<UUID, Deque<PricingEvaluationLine>> queues = new LinkedHashMap<>();
        for (PricingEvaluationLine line : pricingEvaluation.getLines()) {
            queues.computeIfAbsent(line.getProductVariant().getId(), ignored -> new ArrayDeque<>()).add(line);
        }
        return queues;
    }

    private PricingEvaluationLine popPricedLine(Map<UUID, Deque<PricingEvaluationLine>> queues, UUID variantId) {
        Deque<PricingEvaluationLine> lines = queues.get(variantId);
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("Pricing line mismatch for variant " + variantId);
        }
        return lines.removeFirst();
    }

    private String buildStorefrontOrderNotes(StorefrontCheckoutRequest request) {
        List<String> parts = new ArrayList<>();
        parts.add("Storefront checkout");
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            parts.add("Customer name: " + request.getCustomerName().trim());
        }
        if (request.getContactName() != null && !request.getContactName().isBlank()) {
            parts.add("Contact name: " + request.getContactName().trim());
        }
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
            parts.add("Email: " + request.getCustomerEmail().trim());
        }
        if (request.getCustomerPhoneNumber() != null && !request.getCustomerPhoneNumber().isBlank()) {
            parts.add("Phone: " + request.getCustomerPhoneNumber().trim());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            parts.add("Payment intent: " + request.getPaymentMethod().trim());
        }
        if (request.getPaymentReference() != null && !request.getPaymentReference().isBlank()) {
            parts.add("Payment reference: " + request.getPaymentReference().trim());
        }
        if (request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
            parts.add("Shipping address: " + request.getShippingAddress().trim());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            parts.add("Customer notes: " + request.getNotes().trim());
        }
        return String.join("\n", parts);
    }

    private BigDecimal computeShippingAmount(BigDecimal netSubtotal) {
        Map<String, Object> siteConfig = readSiteConfig();
        Object rawShipping = siteConfig.get("shippingFlatRate");
        if (rawShipping != null) {
            BigDecimal flatRate = new BigDecimal(rawShipping.toString());
            if (flatRate.compareTo(BigDecimal.ZERO) > 0) {
                Object rawThreshold = siteConfig.get("freeShippingThreshold");
                if (rawThreshold != null) {
                    BigDecimal threshold = new BigDecimal(rawThreshold.toString());
                    if (netSubtotal.compareTo(threshold) >= 0) {
                        return BigDecimal.ZERO;
                    }
                }
                return flatRate;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal computeTaxAmount(BigDecimal netSubtotal) {
        Map<String, Object> siteConfig = readSiteConfig();
        Object rawTaxRate = siteConfig.get("taxRate");
        if (rawTaxRate != null) {
            BigDecimal taxRate = new BigDecimal(rawTaxRate.toString());
            if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
                return netSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSiteConfig() {
        try {
            Map<String, Object> doc = readSetting(DRAFT_THEME_DOCUMENT_KEY, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}, null);
            if (doc != null && doc.get("settings") instanceof Map) {
                Map<String, Object> settings = (Map<String, Object>) doc.get("settings");
                if (settings.get("site") instanceof Map) {
                    return (Map<String, Object>) settings.get("site");
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Map.of();
    }

    private void reserveStockForOrder(SalesOrder order, Warehouse warehouse) {
        org.springframework.transaction.support.TransactionTemplate isolated =
                new org.springframework.transaction.support.TransactionTemplate(platformTransactionManager);
        isolated.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (SalesOrderItem item : order.getItems()) {
            try {
                isolated.executeWithoutResult(status -> {
                    StockReservationRequest reservationRequest = new StockReservationRequest();
                    reservationRequest.setProductVariantId(item.getProductVariant().getId());
                    reservationRequest.setWarehouseId(warehouse.getId());
                    reservationRequest.setQuantity(item.getQuantity());
                    reservationRequest.setReferenceId(order.getSoNumber());
                    reservationRequest.setPriority(com.inventory.system.common.entity.ReservationPriority.HIGH);
                    reservationRequest.setExpiresAt(LocalDateTime.now().plusHours(48));
                    stockReservationService.reserveStock(reservationRequest);
                });
            } catch (Exception e) {
                logger.warn("Stock reservation skipped for order {} variant {}: {}",
                        order.getSoNumber(), item.getProductVariant().getId(), e.getMessage());
            }
        }
    }

    private String generateStorefrontOrderNumber() {
        String prefix = "SO-WEB-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-";
        String candidate;
        do {
            candidate = prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (salesOrderRepository.existsBySoNumber(candidate));
        return candidate;
    }

    private SalesOrderDto mapSalesOrder(SalesOrder order) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setId(order.getId());
        dto.setSoNumber(order.getSoNumber());
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer().getName());
        dto.setWarehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null);
        dto.setWarehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null);
        dto.setOrderDate(order.getOrderDate());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        dto.setStatus(order.getStatus());
        dto.setPriority(order.getPriority());
        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setShippingAmount(order.getShippingAmount());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setSalesChannel(order.getSalesChannel());
        dto.setAppliedCouponCodes(order.getAppliedCouponCodes());
        dto.setCurrency(order.getCurrency());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setCreatedBy(order.getCreatedBy());
        dto.setUpdatedBy(order.getUpdatedBy());
        dto.setItems(order.getItems().stream().map(this::mapSalesOrderItem).toList());
        return dto;
    }

    private SalesOrderItemDto mapSalesOrderItem(SalesOrderItem item) {
        SalesOrderItemDto dto = new SalesOrderItemDto();
        dto.setId(item.getId());
        dto.setProductVariantId(item.getProductVariant().getId());
        dto.setProductVariantName(item.getProductVariant().getTemplate().getName());
        dto.setSku(item.getProductVariant().getSku());
        dto.setQuantity(item.getQuantity());
        dto.setBaseUnitPrice(item.getBaseUnitPrice());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineDiscount(item.getLineDiscount());
        dto.setAppliedPromotionCodes(item.getAppliedPromotionCodes());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setShippedQuantity(item.getShippedQuantity());
        return dto;
    }

    private String slugify(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        String slug = SLUG_NON_ALNUM.matcher(normalized).replaceAll("-");
        return slug.replaceAll("(^-|-$)", "");
    }

    private StorefrontSiteDto defaultSite() {
        return new StorefrontSiteDto(
                true,
                "boutique",
                "Marl & Manor",
                "Everyday essentials with a sharper point of view.",
                "Shop our latest arrivals!",
                "marlandmanor.store",
                "Storefront Pro",
                "/logistra.svg",
                "/logistra.svg",
                40,
                "SECOND_IMAGE",
                true,
                "Categories",
                "Shop by category",
                "Catalog",
                "Browse Men, Women, Accessories, and all storefront subcategories.",
                "All collections",
                "Search the catalog",
                "Cart",
                null,
                "Catalog",
                "Collections",
                "Track order",
                "DRAFT",
                null,
                null
        );
    }

    private StorefrontThemeDto defaultTheme() {
        return new StorefrontThemeDto(
                "#00322d",
                "#004b44",
                "#4a200e",
                "#f8f9fa",
                "#191c1d",
                24,
                "left",
                "Manrope",
                "Inter"
        );
    }

    private List<StorefrontNavItemDto> defaultNavigation() {
        return List.of(
                new StorefrontNavItemDto("nav-1", "Home", "/"),
                new StorefrontNavItemDto("nav-2", "Collections", "/collections"),
                new StorefrontNavItemDto("nav-3", "Products", "/products"),
                new StorefrontNavItemDto("nav-4", "Contact", "/contact")
        );
    }

    private List<StorefrontBannerDto> defaultBanners() {
        return List.of(
                new StorefrontBannerDto("banner-1", "Spring campaign", "hero", true),
                new StorefrontBannerDto("banner-2", "Warehouse bundle promo", "between-sections", true)
        );
    }

    private StorefrontPageDto defaultHomePage() {
        return new StorefrontPageDto(
                "home",
                "Home page",
                List.of(
                        new StorefrontSectionDto("hero", "Hero banner", "hero_banner", true, "brand-split",
                                new LinkedHashMap<>(java.util.Map.of(
                                        "eyebrow", "New Arrivals",
                                        "headline", "New Arrivals",
                                        "subheadline", "Let your day be filled with what inspires you.",
                                        "ctaLabel", "Shop our latest arrivals!",
                                        "ctaHref", "/products",
                                        "secondaryCtaLabel", "View collections",
                                        "secondaryCtaHref", "/collections",
                                        "imageUrl", "https://marlandmanor.store/cdn/shop/files/Black_And_Gold_Elegant_Fashion_Logo_Facebook_Cover_1.png?v=1769370822&width=3840"
                                ))),
                        new StorefrontSectionDto("promo", "Promo strip", "promo_strip", true, "three-up",
                                new LinkedHashMap<>(java.util.Map.of(
                                        "items", List.of(
                                                "Inventory-backed catalog",
                                                "Config-managed merchandising",
                                                "Optional storefront module"
                                        )
                                ))),
                        new StorefrontSectionDto("new-arrivals", "New arrivals", "featured_products", true, "new-arrivals",
                                new LinkedHashMap<>(java.util.Map.of(
                                        "source", "newest",
                                        "limit", 8,
                                        "eyebrow", "New Arrivals",
                                        "title", "New Arrivals",
                                        "description", "Let your day be filled with what inspires you.",
                                        "ctaLabel", "Shop all products"
                                ))),
                        new StorefrontSectionDto("bestsellers", "Best sellers", "featured_products", true, "bestsellers",
                                new LinkedHashMap<>(java.util.Map.of(
                                        "source", "featured",
                                        "limit", 4,
                                        "eyebrow", "Our Bestsellers",
                                        "title", "OUR BESTSELLERS",
                                        "description", "FUNCTIONAL EVERYDAY ESSENTIALS",
                                        "ctaLabel", "View all products"
                                ))),
                        new StorefrontSectionDto("collections", "Collection list", "featured_collections", true, "cards",
                                new LinkedHashMap<>(java.util.Map.of(
                                        "limit", 4,
                                        "collectionSlugs", List.of(),
                                        "eyebrow", "Collections",
                                        "title", "Collections",
                                        "description", "Choose which collections to feature on the homepage.",
                                        "ctaLabel", "Browse all collections",
                                        "collectionKickerLabel", "Curated space",
                                        "collectionCardCtaLabel", "Explore collection"
                                )))
                )
        );
    }

    private StorefrontPageDto defaultHeaderPage() {
        return buildHeaderPage(defaultSite(), defaultNavigation());
    }

    private StorefrontPageDto defaultFooterPage() {
        return buildFooterPage(defaultSite());
    }

    private StorefrontPageDto buildHeaderPage(StorefrontSiteDto site, List<StorefrontNavItemDto> navigationItems) {
        Map<String, Object> announcementConfig = new LinkedHashMap<>();
        announcementConfig.put("text", site.getAnnouncement());
        announcementConfig.put("enabled", site.getAnnouncement() != null && !site.getAnnouncement().isBlank());

        Map<String, Object> navigationConfig = new LinkedHashMap<>();
        navigationConfig.put("drawerButtonLabel", site.getDrawerButtonLabel());
        navigationConfig.put("drawerLabel", site.getDrawerLabel());
        navigationConfig.put("drawerTitle", site.getDrawerTitle());
        navigationConfig.put("drawerDescription", site.getDrawerDescription());
        navigationConfig.put("allCollectionsLabel", site.getAllCollectionsLabel());
        navigationConfig.put("searchPlaceholder", site.getSearchPlaceholder());
        navigationConfig.put("cartLabel", site.getCartLabel());
        navigationConfig.put("navBlockIds", navigationItems.stream().map(StorefrontNavItemDto::getId).toList());

        return new StorefrontPageDto(
                "header",
                "Header",
                List.of(
                        new StorefrontSectionDto(
                                "header-announcement",
                                "Announcement bar",
                                "announcement_bar",
                                site.getAnnouncement() != null && !site.getAnnouncement().isBlank(),
                                "default",
                                announcementConfig
                        ),
                        new StorefrontSectionDto(
                                "header-navigation",
                                "Header navigation",
                                "header_nav",
                                true,
                                "default",
                                navigationConfig
                        )
                )
        );
    }

    private StorefrontPageDto buildFooterPage(StorefrontSiteDto site) {
        Map<String, Object> footerConfig = new LinkedHashMap<>();
        footerConfig.put("brandName", site.getName());
        footerConfig.put("tagline", site.getTagline());
        footerConfig.put("linkBlockIds", List.of("footer-link-catalog", "footer-link-collections", "footer-link-tracking"));

        return new StorefrontPageDto(
                "footer",
                "Footer",
                List.of(
                        new StorefrontSectionDto(
                                "footer-main",
                                "Footer links",
                                "footer_links",
                                true,
                                "default",
                                footerConfig
                        )
                )
        );
    }

    private StorefrontCollectionDto mapCollection(Category category, Map<UUID, List<Category>> childrenByParent, int level) {
        return StorefrontCollectionDto.builder()
                .id(category.getId().toString())
                .slug(resolveCategorySlug(category))
                .title(resolveCategoryTitle(category))
                .description(resolveCategoryDescription(category))
                .sortOrder(category.getStorefrontSortOrder())
                .parentId(category.getParent() != null && Boolean.TRUE.equals(category.getParent().getPublishedToStorefront())
                        ? category.getParent().getId().toString()
                        : null)
                .parentSlug(category.getParent() != null && Boolean.TRUE.equals(category.getParent().getPublishedToStorefront())
                        ? resolveCategorySlug(category.getParent())
                        : null)
                .level(level)
                .children(childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                        .map(child -> mapCollection(child, childrenByParent, level + 1))
                        .toList())
                .build();
    }

    private String resolveCategorySlug(Category category) {
        if (category == null) {
            return null;
        }
        return category.getStorefrontSlug() != null && !category.getStorefrontSlug().isBlank()
                ? category.getStorefrontSlug()
                : slugify(category.getName());
    }

    private String resolveCategoryTitle(Category category) {
        if (category == null) {
            return null;
        }
        return category.getStorefrontTitle() != null && !category.getStorefrontTitle().isBlank()
                ? category.getStorefrontTitle()
                : category.getName();
    }

    private String resolveCategoryDescription(Category category) {
        if (category == null) {
            return null;
        }
        return category.getStorefrontDescription() != null && !category.getStorefrontDescription().isBlank()
                ? category.getStorefrontDescription()
                : category.getDescription();
    }

    private String resolveParentCollectionSlug(Category category) {
        if (category == null || category.getParent() == null || !Boolean.TRUE.equals(category.getParent().getPublishedToStorefront())) {
            return null;
        }
        return resolveCategorySlug(category.getParent());
    }

    private String resolveParentCollectionTitle(Category category) {
        if (category == null || category.getParent() == null || !Boolean.TRUE.equals(category.getParent().getPublishedToStorefront())) {
            return null;
        }
        return resolveCategoryTitle(category.getParent());
    }

    private String buildCategoryPath(Category category) {
        if (category == null) {
            return null;
        }
        return String.join(" / ", buildCategoryTrailTitles(category));
    }

    private List<String> buildCategoryTrailSlugs(Category category) {
        List<String> trail = new ArrayList<>();
        Category current = category;
        while (current != null) {
            if (Boolean.TRUE.equals(current.getPublishedToStorefront())) {
                trail.add(0, resolveCategorySlug(current));
            }
            current = current.getParent();
        }
        return trail;
    }

    private List<String> buildCategoryTrailTitles(Category category) {
        List<String> trail = new ArrayList<>();
        Category current = category;
        while (current != null) {
            if (Boolean.TRUE.equals(current.getPublishedToStorefront())) {
                trail.add(0, resolveCategoryTitle(current));
            }
            current = current.getParent();
        }
        return trail;
    }

    private String resolveStorefrontImageUrl(ProductImage image) {
        if (image == null || image.getId() == null) {
            return null;
        }
        return "/api/v1/product-images/" + image.getId() + "/file";
    }
}
