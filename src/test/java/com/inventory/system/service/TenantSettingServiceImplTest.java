package com.inventory.system.service;

import com.inventory.system.common.entity.TenantSetting;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.repository.TenantSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSettingServiceImplTest {

    @Mock
    private TenantSettingRepository tenantSettingRepository;

    @InjectMocks
    private TenantSettingServiceImpl tenantSettingService;

    private MockedStatic<TenantContext> tenantContextMock;

    @BeforeEach
    void setUp() {
        tenantContextMock = Mockito.mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant-123");
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void findSetting_UsesCurrentTenantContext() {
        TenantSetting setting = new TenantSetting();
        setting.setId(UUID.randomUUID());
        setting.setTenantId("tenant-123");
        setting.setSettingKey("storefront.site");
        setting.setSettingValue("{}");
        setting.setSettingType("JSON");
        setting.setCategory("STOREFRONT");

        when(tenantSettingRepository.findByTenantIdAndSettingKey("tenant-123", "storefront.site"))
                .thenReturn(Optional.of(setting));

        Optional<TenantSettingDto> result = tenantSettingService.findSetting("storefront.site");

        assertTrue(result.isPresent());
        assertEquals("storefront.site", result.get().getKey());
        verify(tenantSettingRepository).findByTenantIdAndSettingKey("tenant-123", "storefront.site");
    }

    @Test
    void updateSetting_UsesCurrentTenantContext() {
        when(tenantSettingRepository.findByTenantIdAndSettingKey("tenant-123", "storefront.activeRevisionId"))
                .thenReturn(Optional.empty());
        when(tenantSettingRepository.save(any(TenantSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantSettingDto result = tenantSettingService.updateSetting(
                "storefront.activeRevisionId",
                "revision-7",
                "STRING",
                "STOREFRONT");

        assertEquals("storefront.activeRevisionId", result.getKey());
        assertEquals("revision-7", result.getValue());
        verify(tenantSettingRepository).findByTenantIdAndSettingKey("tenant-123", "storefront.activeRevisionId");
    }

    @Test
    void getSettings_ReturnsOnlyCurrentTenantSettings() {
        TenantSetting setting = new TenantSetting();
        setting.setId(UUID.randomUUID());
        setting.setTenantId("tenant-123");
        setting.setSettingKey("storefront.theme");
        setting.setSettingValue("{}");
        setting.setSettingType("JSON");
        setting.setCategory("STOREFRONT");

        when(tenantSettingRepository.findByTenantIdAndCategoryOrderBySettingKeyAsc("tenant-123", "STOREFRONT"))
                .thenReturn(List.of(setting));

        List<TenantSettingDto> result = tenantSettingService.getSettings("STOREFRONT");

        assertEquals(1, result.size());
        assertEquals("storefront.theme", result.get(0).getKey());
        verify(tenantSettingRepository).findByTenantIdAndCategoryOrderBySettingKeyAsc("tenant-123", "STOREFRONT");
    }
}