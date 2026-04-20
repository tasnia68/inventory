package com.inventory.system.controller;

import com.inventory.system.common.exception.StorefrontModuleDisabledException;
import com.inventory.system.common.exception.StorefrontModuleUnavailableException;
import com.inventory.system.service.StorefrontDomainService;
import com.inventory.system.service.StorefrontService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "minio.url=http://localhost:9000",
    "minio.access-key=test-access-key",
    "minio.secret-key=test-secret-key",
    "minio.bucket=test-bucket"
})
@AutoConfigureMockMvc
class StorefrontControllerFeatureGateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorefrontService storefrontService;

    @MockBean
    private StorefrontDomainService storefrontDomainService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser(authorities = "MENU:ANALYTICS")
    void getAdminConfig_returnsForbiddenWhenStorefrontModuleDisabled() throws Exception {
        when(storefrontService.getAdminConfig()).thenThrow(new StorefrontModuleDisabledException());

        mockMvc.perform(get("/api/v1/storefront/config")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getPublicConfig_returnsNotFoundWhenStorefrontModuleDisabled() throws Exception {
        when(storefrontDomainService.isLocalDevelopmentHost("localhost")).thenReturn(true);
        when(storefrontDomainService.resolveTenantIdForHost("tenant-a.local")).thenReturn(Optional.of("tenant-a"));
        when(storefrontService.getPublicConfig()).thenThrow(new StorefrontModuleUnavailableException());

        mockMvc.perform(get("/api/v1/storefront/public/config")
                        .header("X-Storefront-Host", "tenant-a.local"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getAssetFile_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/storefront/assets/file")
                        .header("X-Tenant-ID", "tenant-a")
                        .param("path", "storefront-assets/tenant-a/hero.png")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateCaddyDomain_returnsForbiddenWhenTenantStorefrontIsDisabled() throws Exception {
        when(storefrontService.allowCaddyDomain("shop.example.com")).thenReturn(false);

        mockMvc.perform(get("/api/v1/storefront/domains/caddy/validate")
                        .param("domain", "shop.example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateCaddyDomain_returnsOkWhenTenantStorefrontIsEnabled() throws Exception {
        when(storefrontService.allowCaddyDomain("shop.example.com")).thenReturn(true);

        mockMvc.perform(get("/api/v1/storefront/domains/caddy/validate")
                        .param("domain", "shop.example.com"))
                .andExpect(status().isOk());
    }
}