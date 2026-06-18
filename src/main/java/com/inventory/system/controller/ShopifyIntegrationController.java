package com.inventory.system.controller;

import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.ShopifyConnectionDto;
import com.inventory.system.payload.ShopifySyncResultDto;
import com.inventory.system.service.ShopifyIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/integrations/shopify")
@RequiredArgsConstructor
public class ShopifyIntegrationController {

    private final ShopifyIntegrationService shopifyIntegrationService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopifyConnectionDto>> getConnection(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shopifyIntegrationService.getConnection(publicBaseUrl(request)),
                "Shopify connection retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopifyConnectionDto>> saveConnection(
            @RequestBody ShopifyConnectionDto requestBody,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shopifyIntegrationService.saveConnection(requestBody, publicBaseUrl(request)),
                "Shopify connection saved"));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<ShopifyConnectionDto>> testConnection(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shopifyIntegrationService.testConnection(publicBaseUrl(request)),
                "Shopify connection tested"));
    }

    @PostMapping("/oauth/start")
    public ResponseEntity<ApiResponse<ShopifyConnectionDto>> startOAuthInstall(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                shopifyIntegrationService.startOAuthInstall(publicBaseUrl(request)),
                "Shopify install URL generated"));
    }

    @GetMapping("/oauth/callback")
    public RedirectView completeOAuthInstall(HttpServletRequest request) {
        return new RedirectView(shopifyIntegrationService.completeOAuthInstall(
                request.getParameterMap(),
                publicBaseUrl(request)));
    }

    @PostMapping("/sync/products")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> syncProducts() {
        ShopifySyncResultDto result = shopifyIntegrationService.syncProducts();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @PostMapping("/sync/orders")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> syncOrders() {
        ShopifySyncResultDto result = shopifyIntegrationService.syncOrders();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @PostMapping("/sync/locations")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> syncLocations() {
        ShopifySyncResultDto result = shopifyIntegrationService.syncLocations();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @PostMapping("/sync/inventory")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> syncInventoryLevels() {
        ShopifySyncResultDto result = shopifyIntegrationService.syncInventoryLevels();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @PostMapping("/push/catalog")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> pushCatalog() {
        ShopifySyncResultDto result = shopifyIntegrationService.pushCatalog();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @PostMapping("/push/inventory")
    public ResponseEntity<ApiResponse<ShopifySyncResultDto>> pushInventory() {
        ShopifySyncResultDto result = shopifyIntegrationService.pushInventory();
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    private String publicBaseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}