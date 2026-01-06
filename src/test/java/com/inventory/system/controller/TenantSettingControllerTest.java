package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.TenantSettingDto;
import com.inventory.system.service.TenantSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TenantSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantSettingService tenantSettingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void testGetSettings() throws Exception {
        TenantSettingDto setting = TenantSettingDto.builder()
                .key("test.key")
                .value("test.value")
                .type("STRING")
                .category("GENERAL")
                .build();

        when(tenantSettingService.getSettings(any())).thenReturn(Collections.singletonList(setting));

        mockMvc.perform(get("/api/v1/settings")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].key").value("test.key"));
    }

    @Test
    @WithMockUser
    public void testGetSetting() throws Exception {
        TenantSettingDto setting = TenantSettingDto.builder()
                .key("test.key")
                .value("test.value")
                .type("STRING")
                .category("GENERAL")
                .build();

        when(tenantSettingService.getSetting(anyString())).thenReturn(setting);

        mockMvc.perform(get("/api/v1/settings/test.key")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("test.key"));
    }

    @Test
    @WithMockUser
    public void testUpdateSetting() throws Exception {
        TenantSettingDto setting = TenantSettingDto.builder()
                .key("test.key")
                .value("new.value")
                .type("STRING")
                .category("GENERAL")
                .build();

        when(tenantSettingService.updateSetting(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(setting);

        mockMvc.perform(put("/api/v1/settings/test.key")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value("new.value"));
    }

    @Test
    @WithMockUser
    public void testUpdateSettings() throws Exception {
        TenantSettingDto setting = TenantSettingDto.builder()
                .key("test.key")
                .value("new.value")
                .type("STRING")
                .category("GENERAL")
                .build();
        List<TenantSettingDto> settings = Collections.singletonList(setting);

        mockMvc.perform(put("/api/v1/settings")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
