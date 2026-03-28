package com.inventory.system.service;

import com.inventory.system.payload.StorefrontConfigDto;
import com.inventory.system.payload.StorefrontCollectionDto;
import com.inventory.system.payload.StorefrontCartDto;
import com.inventory.system.payload.StorefrontCartRequest;
import com.inventory.system.payload.StorefrontCheckoutDto;
import com.inventory.system.payload.StorefrontCheckoutRequest;
import com.inventory.system.payload.StorefrontAssetUploadDto;
import com.inventory.system.payload.StorefrontAccountAuthDto;
import com.inventory.system.payload.StorefrontAccountOrderDto;
import com.inventory.system.payload.StorefrontAccountProfileDto;
import com.inventory.system.payload.StorefrontAccountUpdateRequest;
import com.inventory.system.payload.StorefrontOrderLookupRequest;
import com.inventory.system.payload.StorefrontOrderTrackingDto;
import com.inventory.system.payload.StorefrontPublishVersionDto;
import com.inventory.system.payload.StorefrontProductPageDto;
import com.inventory.system.payload.StorefrontProductDto;
import com.inventory.system.payload.StorefrontPublishRequest;
import com.inventory.system.payload.StorefrontLoginChallengeDto;
import com.inventory.system.payload.StorefrontLoginRequest;
import com.inventory.system.payload.StorefrontLoginVerifyRequest;
import com.inventory.system.payload.StorefrontThemeDocumentDto;
import com.inventory.system.payload.StorefrontThemeEditorDto;
import com.inventory.system.payload.UpdateStorefrontConfigRequest;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface StorefrontService {
    StorefrontConfigDto getAdminConfig();
    StorefrontConfigDto updateConfig(UpdateStorefrontConfigRequest request);
    StorefrontConfigDto getPublicConfig();
    List<StorefrontCollectionDto> getPublicCollections();
    StorefrontProductPageDto getPublicProducts(String query, String collectionSlug, String sort, Integer page, Integer size);
    StorefrontProductDto getPublicProduct(String slug);
    StorefrontCartDto previewCart(StorefrontCartRequest request);
    StorefrontCheckoutDto checkout(StorefrontCheckoutRequest request);
    StorefrontOrderTrackingDto lookupOrder(StorefrontOrderLookupRequest request);
    List<StorefrontPublishVersionDto> getPublishVersions();
    StorefrontPublishVersionDto publishCurrentConfig(StorefrontPublishRequest request);
    StorefrontPublishVersionDto rollbackToVersion(UUID versionId, StorefrontPublishRequest request);
    StorefrontAssetUploadDto uploadAsset(MultipartFile file, String assetType);
    StorefrontThemeEditorDto getAdminThemeEditor();
    StorefrontThemeDocumentDto updateDraftTheme(StorefrontThemeDocumentDto request);
    List<StorefrontPublishVersionDto> getThemeRevisions();
    StorefrontPublishVersionDto publishDraftTheme(StorefrontPublishRequest request);
    StorefrontPublishVersionDto restoreThemeRevision(UUID versionId, StorefrontPublishRequest request);
    StorefrontConfigDto getAdminThemePreview();
    StorefrontLoginChallengeDto requestAccountLogin(StorefrontLoginRequest request);
    StorefrontAccountAuthDto verifyAccountLogin(StorefrontLoginVerifyRequest request);
    StorefrontAccountProfileDto getAccountProfile(String sessionToken);
    StorefrontAccountProfileDto updateAccountProfile(String sessionToken, StorefrontAccountUpdateRequest request);
    List<StorefrontAccountOrderDto> getAccountOrders(String sessionToken);
    void logoutAccount(String sessionToken);
}
