package com.inventory.system.service;

import com.inventory.system.common.exception.StorefrontModuleDisabledException;
import com.inventory.system.common.exception.StorefrontModuleUnavailableException;
import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.repository.CategoryRepository;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.ProductVariantRepository;
import com.inventory.system.repository.SalesOrderRepository;
import com.inventory.system.repository.StorefrontAccountRepository;
import com.inventory.system.repository.StorefrontAccountSessionRepository;
import com.inventory.system.repository.StorefrontLoginChallengeRepository;
import com.inventory.system.repository.StorefrontPublishVersionRepository;
import com.inventory.system.repository.TenantSettingRepository;
import com.inventory.system.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorefrontServiceImplFeatureGateTest {

    private static final String STOREFRONT_MODULE_KEY = "tenant.modules.storefront.enabled";
    private static final String TENANT_ID = "tenant-a";

    @Mock
    private TenantSettingService tenantSettingService;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private StorefrontAccountRepository storefrontAccountRepository;

    @Mock
    private StorefrontLoginChallengeRepository storefrontLoginChallengeRepository;

    @Mock
    private StorefrontAccountSessionRepository storefrontAccountSessionRepository;

    @Mock
    private PricingEngineService pricingEngineService;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private StorefrontPublishVersionRepository storefrontPublishVersionRepository;

    @Mock
    private TenantSettingRepository tenantSettingRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EmailService emailService;

    @Mock
    private StorefrontDomainService storefrontDomainService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StorefrontServiceImpl storefrontService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getAdminConfig_throwsForbiddenWhenStorefrontModuleDisabled() {
        TenantContext.setTenantId(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.of(settingWithValue("false")));

        assertThrows(StorefrontModuleDisabledException.class, storefrontService::getAdminConfig);
        verifyNoInteractions(storefrontPublishVersionRepository, storefrontDomainService);
    }

    @Test
    void getPublicConfig_throwsNotFoundWhenStorefrontModuleDisabled() {
        TenantContext.setTenantId(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.of(settingWithValue("false")));

        assertThrows(StorefrontModuleUnavailableException.class, storefrontService::getPublicConfig);
        verifyNoInteractions(storefrontPublishVersionRepository, storefrontDomainService);
    }

    @Test
    void allowCaddyDomain_returnsTrueWhenResolvedTenantStorefrontIsEnabled() {
        when(storefrontDomainService.isDomainAllowedForCaddy("shop.example.com")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("shop.example.com")).thenReturn(Optional.of(TENANT_ID));
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.of(settingWithValue("true")));

        assertDoesNotThrow(() -> assertTrue(storefrontService.allowCaddyDomain("shop.example.com")));
    }

    @Test
    void getAdminConfig_throwsForbiddenWhenStorefrontModuleSettingMissing() {
        TenantContext.setTenantId(TENANT_ID);
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.empty());

        assertThrows(StorefrontModuleDisabledException.class, storefrontService::getAdminConfig);
    }

    @Test
    void allowCaddyDomain_returnsFalseWhenResolvedTenantStorefrontIsDisabled() {
        when(storefrontDomainService.isDomainAllowedForCaddy("shop.example.com")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("shop.example.com")).thenReturn(Optional.of(TENANT_ID));
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.of(settingWithValue("false")));

        assertFalse(storefrontService.allowCaddyDomain("shop.example.com"));
    }

    @Test
    void allowCaddyDomain_returnsFalseWhenResolvedTenantStorefrontSettingMissing() {
        when(storefrontDomainService.isDomainAllowedForCaddy("shop.example.com")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("shop.example.com")).thenReturn(Optional.of(TENANT_ID));
        when(tenantSettingRepository.findByTenantIdAndSettingKey(TENANT_ID, STOREFRONT_MODULE_KEY))
                .thenReturn(Optional.empty());

        assertFalse(storefrontService.allowCaddyDomain("shop.example.com"));
    }

    @Test
    void allowCaddyDomain_allowsPlatformDomainWhenNoTenantBindingIsRequired() {
        when(storefrontDomainService.isDomainAllowedForCaddy("admin.example.com")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("admin.example.com")).thenReturn(Optional.empty());

        assertTrue(storefrontService.allowCaddyDomain("admin.example.com"));
        verifyNoInteractions(tenantSettingRepository);
    }

    private TenantSetting settingWithValue(String value) {
        TenantSetting setting = new TenantSetting();
        setting.setSettingKey(STOREFRONT_MODULE_KEY);
        setting.setSettingValue(value);
        setting.setSettingType("BOOLEAN");
        setting.setTenantId(TENANT_ID);
        return setting;
    }
}