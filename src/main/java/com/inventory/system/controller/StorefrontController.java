package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.StorefrontCartDto;
import com.inventory.system.payload.StorefrontCartRequest;
import com.inventory.system.payload.StorefrontCheckoutDto;
import com.inventory.system.payload.StorefrontCheckoutRequest;
import com.inventory.system.payload.StorefrontCmsPageDto;
import com.inventory.system.payload.StorefrontCmsPageRequest;
import com.inventory.system.payload.StorefrontAssetUploadDto;
import com.inventory.system.payload.StorefrontAccountAuthDto;
import com.inventory.system.payload.StorefrontAccountOrderDto;
import com.inventory.system.payload.StorefrontAccountProfileDto;
import com.inventory.system.payload.StorefrontAccountUpdateRequest;
import com.inventory.system.payload.StorefrontAnalyticsDto;
import com.inventory.system.payload.StorefrontConfigDto;
import com.inventory.system.payload.StorefrontCollectionDto;
import com.inventory.system.payload.StorefrontDomainContextDto;
import com.inventory.system.payload.StorefrontDomainDto;
import com.inventory.system.payload.StorefrontDomainRequest;
import com.inventory.system.payload.StorefrontLoginChallengeDto;
import com.inventory.system.payload.StorefrontLoginRequest;
import com.inventory.system.payload.StorefrontLoginVerifyRequest;
import com.inventory.system.payload.StorefrontOrderLookupRequest;
import com.inventory.system.payload.StorefrontOrderTrackingDto;
import com.inventory.system.payload.StorefrontPublishRequest;
import com.inventory.system.payload.StorefrontPublishVersionDto;
import com.inventory.system.payload.StorefrontProductPageDto;
import com.inventory.system.payload.StorefrontProductDto;
import com.inventory.system.payload.StorefrontThemeDocumentDto;
import com.inventory.system.payload.StorefrontThemeEditorDto;
import com.inventory.system.payload.StorefrontThemeManifestDto;
import com.inventory.system.payload.UpdateStorefrontConfigRequest;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.service.FileStorageService;
import com.inventory.system.service.StorefrontService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class StorefrontController {

    private final StorefrontService storefrontService;
    private final FileStorageService fileStorageService;
    private final com.inventory.system.service.VirtualTryOnService virtualTryOnService;

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getAdminConfig() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminConfig()));
    }

    @GetMapping("/admin/theme")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontThemeEditorDto>> getAdminThemeEditor() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminThemeEditor()));
    }

    @GetMapping("/admin/themes/registry")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<List<StorefrontThemeManifestDto>>> listAvailableThemes() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.listAvailableThemes()));
    }

    @GetMapping("/admin/themes/{themeKey}")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontThemeManifestDto>> getThemeManifest(@PathVariable String themeKey) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getThemeManifest(themeKey)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/themes/{themeKey}/activate")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontThemeEditorDto>> activateTheme(@PathVariable String themeKey) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.activateTheme(themeKey), "Theme activated"));
    }

    @GetMapping("/admin/domains")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontDomainContextDto>> getDomainContext() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getDomainContext()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/domains")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontDomainDto>> addDomain(@Valid @RequestBody StorefrontDomainRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.addDomain(request), "Storefront domain added successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/domains/{domainId}/verify")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontDomainDto>> verifyDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.verifyDomain(domainId), "Storefront domain verified"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/domains/{domainId}/activate")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontDomainDto>> activateDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.activateDomain(domainId), "Storefront domain activated"));
    }

    @DeleteMapping("/admin/domains/{domainId}")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<Void>> removeDomain(@PathVariable UUID domainId) {
        storefrontService.removeDomain(domainId);
        return ResponseEntity.ok(ApiResponse.success(null, "Storefront domain removed"));
    }

    @PutMapping("/admin/theme/draft")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontThemeDocumentDto>> updateDraftTheme(@RequestBody StorefrontThemeDocumentDto request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateDraftTheme(request), "Storefront draft theme updated successfully"));
    }

    @GetMapping("/admin/theme/revisions")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<List<StorefrontPublishVersionDto>>> getThemeRevisions() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getThemeRevisions()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/theme/revisions")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> publishDraftTheme(@RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.publishDraftTheme(request), "Storefront theme published successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/theme/revisions/{versionId}/restore")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> restoreThemeRevision(
            @PathVariable UUID versionId,
            @RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.restoreThemeRevision(versionId, request), "Storefront theme restored successfully"));
    }

    @GetMapping("/admin/theme/preview")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getAdminThemePreview() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminThemePreview()));
    }

    @GetMapping("/admin/customers")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<List<StorefrontAccountProfileDto>>> listStorefrontCustomers() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.listStorefrontAccounts()));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontAnalyticsDto>> getStorefrontAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getStorefrontAnalytics(from, to)));
    }

    // ── CMS Pages Admin ─────────────────────────────────────────

    @GetMapping("/admin/pages")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<List<StorefrontCmsPageDto>>> listCmsPages() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.listCmsPages()));
    }

    @GetMapping("/admin/pages/{id}")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontCmsPageDto>> getCmsPage(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getCmsPage(id)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/pages")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontCmsPageDto>> createCmsPage(@Valid @RequestBody StorefrontCmsPageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.createCmsPage(request), "Page created successfully"));
    }

    @PutMapping("/admin/pages/{id}")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontCmsPageDto>> updateCmsPage(@PathVariable UUID id, @Valid @RequestBody StorefrontCmsPageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateCmsPage(id, request), "Page updated successfully"));
    }

    @DeleteMapping("/admin/pages/{id}")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<Void>> deleteCmsPage(@PathVariable UUID id) {
        storefrontService.deleteCmsPage(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Page deleted successfully"));
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> updateConfig(@RequestBody UpdateStorefrontConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateConfig(request), "Storefront config updated successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/assets", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontAssetUploadDto>> uploadAsset(
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(value = "assetType", required = false) String assetType) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.uploadAsset(file, assetType), "Storefront asset uploaded successfully"));
    }

    @GetMapping("/assets/file")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<InputStreamResource> getAssetFile(@RequestParam("path") String path) {
        storefrontService.getAdminConfig();
        validateAssetPath(path);
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(path));
        String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    private void validateAssetPath(String path) {
        String tenantId = TenantContext.getTenantId();
        String expectedPrefix = "storefront-assets/" + tenantId + "/";
        if (path == null || path.isBlank() || tenantId == null || tenantId.isBlank() || !path.startsWith(expectedPrefix)) {
            throw new ResourceNotFoundException("Storefront asset", "path", path);
        }
    }

    @GetMapping("/publish/versions")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<List<StorefrontPublishVersionDto>>> getPublishVersions() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublishVersions()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/publish")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> publishCurrentConfig(@RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.publishCurrentConfig(request), "Storefront published successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/publish/{versionId}/rollback")
    @PreAuthorize("hasAuthority('MENU:ANALYTICS')")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> rollbackToVersion(
            @PathVariable UUID versionId,
            @RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.rollbackToVersion(versionId, request), "Storefront rollback completed successfully"));
    }

    @GetMapping("/public/config")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getPublicConfig() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicConfig()));
    }

    @GetMapping("/domains/caddy/validate")
    public ResponseEntity<Void> validateCaddyDomain(@RequestParam("domain") String domain) {
        return storefrontService.allowCaddyDomain(domain)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(403).build();
    }

    @GetMapping("/public/collections")
    public ResponseEntity<ApiResponse<List<StorefrontCollectionDto>>> getPublicCollections() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicCollections()));
    }

    @GetMapping("/public/products")
    public ResponseEntity<ApiResponse<StorefrontProductPageDto>> getPublicProducts(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false, name = "collection") String collectionSlug,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicProducts(query, collectionSlug, sort, page, size)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/cart/preview")
    public ResponseEntity<ApiResponse<StorefrontCartDto>> previewCart(@Valid @RequestBody StorefrontCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.previewCart(request), "Storefront cart priced successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/virtual-try-on")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> virtualTryOn(@RequestBody java.util.Map<String, Object> body) {
        String tenantId = com.inventory.system.config.tenant.TenantContext.getTenantId();
        java.util.UUID variantId = body.get("productVariantId") != null
                ? java.util.UUID.fromString(String.valueOf(body.get("productVariantId"))) : null;
        String userImageBase64 = body.get("userImageBase64") != null
                ? String.valueOf(body.get("userImageBase64")) : null;
        String customerIdentifier = body.get("customerIdentifier") != null
                ? String.valueOf(body.get("customerIdentifier")) : null;
        com.inventory.system.service.VirtualTryOnService.TryOnResult result =
                virtualTryOnService.requestTryOn(tenantId, customerIdentifier, variantId, userImageBase64);
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
                "imageBase64", result.imageBase64(),
                "mimeType", result.mimeType()
        ), "Try-on generated"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/checkout")
    public ResponseEntity<ApiResponse<StorefrontCheckoutDto>> checkout(@Valid @RequestBody StorefrontCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.checkout(request), "Storefront checkout created successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/orders/lookup")
    public ResponseEntity<ApiResponse<StorefrontOrderTrackingDto>> lookupOrder(@Valid @RequestBody StorefrontOrderLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.lookupOrder(request), "Storefront order retrieved successfully"));
    }

    @GetMapping("/public/products/{slug}")
    public ResponseEntity<ApiResponse<StorefrontProductDto>> getPublicProduct(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicProduct(slug)));
    }

    @GetMapping("/public/pages/{slug}")
    public ResponseEntity<ApiResponse<StorefrontCmsPageDto>> getPublicCmsPage(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicCmsPage(slug)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/account/login/request")
    public ResponseEntity<ApiResponse<StorefrontLoginChallengeDto>> requestAccountLogin(@Valid @RequestBody StorefrontLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.requestAccountLogin(request), "Storefront login code sent successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/account/login/verify")
    public ResponseEntity<ApiResponse<StorefrontAccountAuthDto>> verifyAccountLogin(@Valid @RequestBody StorefrontLoginVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.verifyAccountLogin(request), "Storefront login verified successfully"));
    }

    @GetMapping("/public/account/me")
    public ResponseEntity<ApiResponse<StorefrontAccountProfileDto>> getAccountProfile(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAccountProfile(extractSessionToken(authorization))));
    }

    @PutMapping("/public/account/me")
    public ResponseEntity<ApiResponse<StorefrontAccountProfileDto>> updateAccountProfile(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody StorefrontAccountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateAccountProfile(extractSessionToken(authorization), request), "Storefront profile updated successfully"));
    }

    @GetMapping("/public/account/orders")
    public ResponseEntity<ApiResponse<List<StorefrontAccountOrderDto>>> getAccountOrders(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAccountOrders(extractSessionToken(authorization))));
    }

    @GetMapping("/public/account/orders/{orderNumber}")
    public ResponseEntity<ApiResponse<StorefrontAccountOrderDto>> getAccountOrderDetail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAccountOrderDetail(extractSessionToken(authorization), orderNumber)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/public/account/logout")
    public ResponseEntity<ApiResponse<Void>> logoutAccount(@RequestHeader(name = "Authorization", required = false) String authorization) {
        storefrontService.logoutAccount(extractSessionToken(authorization));
        return ResponseEntity.ok(ApiResponse.success(null, "Storefront account logged out successfully"));
    }

    private String extractSessionToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
