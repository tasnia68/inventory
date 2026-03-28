package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.StorefrontCartDto;
import com.inventory.system.payload.StorefrontCartRequest;
import com.inventory.system.payload.StorefrontCheckoutDto;
import com.inventory.system.payload.StorefrontCheckoutRequest;
import com.inventory.system.payload.StorefrontAssetUploadDto;
import com.inventory.system.payload.StorefrontAccountAuthDto;
import com.inventory.system.payload.StorefrontAccountOrderDto;
import com.inventory.system.payload.StorefrontAccountProfileDto;
import com.inventory.system.payload.StorefrontAccountUpdateRequest;
import com.inventory.system.payload.StorefrontConfigDto;
import com.inventory.system.payload.StorefrontCollectionDto;
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
import com.inventory.system.payload.UpdateStorefrontConfigRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/v1/storefront")
@RequiredArgsConstructor
public class StorefrontController {

    private final StorefrontService storefrontService;
    private final FileStorageService fileStorageService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getAdminConfig() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminConfig()));
    }

    @GetMapping("/admin/theme")
    public ResponseEntity<ApiResponse<StorefrontThemeEditorDto>> getAdminThemeEditor() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminThemeEditor()));
    }

    @PutMapping("/admin/theme/draft")
    public ResponseEntity<ApiResponse<StorefrontThemeDocumentDto>> updateDraftTheme(@RequestBody StorefrontThemeDocumentDto request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateDraftTheme(request), "Storefront draft theme updated successfully"));
    }

    @GetMapping("/admin/theme/revisions")
    public ResponseEntity<ApiResponse<List<StorefrontPublishVersionDto>>> getThemeRevisions() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getThemeRevisions()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/theme/revisions")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> publishDraftTheme(@RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.publishDraftTheme(request), "Storefront theme published successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/theme/revisions/{versionId}/restore")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> restoreThemeRevision(
            @PathVariable UUID versionId,
            @RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.restoreThemeRevision(versionId, request), "Storefront theme restored successfully"));
    }

    @GetMapping("/admin/theme/preview")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getAdminThemePreview() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getAdminThemePreview()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> updateConfig(@RequestBody UpdateStorefrontConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.updateConfig(request), "Storefront config updated successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping(value = "/assets", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<StorefrontAssetUploadDto>> uploadAsset(
            @org.springframework.web.bind.annotation.RequestParam("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(value = "assetType", required = false) String assetType) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.uploadAsset(file, assetType), "Storefront asset uploaded successfully"));
    }

    @GetMapping("/assets/file")
    public ResponseEntity<InputStreamResource> getAssetFile(@RequestParam("path") String path) {
        InputStreamResource resource = new InputStreamResource(fileStorageService.getFile(path));
        String filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/publish/versions")
    public ResponseEntity<ApiResponse<List<StorefrontPublishVersionDto>>> getPublishVersions() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublishVersions()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/publish")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> publishCurrentConfig(@RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.publishCurrentConfig(request), "Storefront published successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/publish/{versionId}/rollback")
    public ResponseEntity<ApiResponse<StorefrontPublishVersionDto>> rollbackToVersion(
            @PathVariable UUID versionId,
            @RequestBody(required = false) StorefrontPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.rollbackToVersion(versionId, request), "Storefront rollback completed successfully"));
    }

    @GetMapping("/public/config")
    public ResponseEntity<ApiResponse<StorefrontConfigDto>> getPublicConfig() {
        return ResponseEntity.ok(ApiResponse.success(storefrontService.getPublicConfig()));
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
