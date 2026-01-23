package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.CycleCountStatus;
import com.inventory.system.common.entity.CycleCountType;
import com.inventory.system.payload.CreateCycleCountRequest;
import com.inventory.system.payload.CycleCountDto;
import com.inventory.system.payload.CycleCountEntryRequest;
import com.inventory.system.service.CycleCountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CycleCountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CycleCountService cycleCountService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCycleCount_ShouldReturnCreated() throws Exception {
        CreateCycleCountRequest request = new CreateCycleCountRequest();
        request.setWarehouseId(UUID.randomUUID());
        request.setType(CycleCountType.FULL);

        CycleCountDto response = new CycleCountDto();
        response.setId(UUID.randomUUID());
        response.setReference("CC-123456");
        response.setStatus(CycleCountStatus.DRAFT);
        response.setType(CycleCountType.FULL);

        when(cycleCountService.createCycleCount(any(CreateCycleCountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cycle-counts")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value("CC-123456"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void getCycleCounts_ShouldReturnPage() throws Exception {
        CycleCountDto dto = new CycleCountDto();
        dto.setReference("CC-123");
        Page<CycleCountDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(cycleCountService.getCycleCounts(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/cycle-counts")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reference").value("CC-123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void startCycleCount_ShouldReturnInProgress() throws Exception {
        UUID id = UUID.randomUUID();
        CycleCountDto dto = new CycleCountDto();
        dto.setId(id);
        dto.setStatus(CycleCountStatus.IN_PROGRESS);

        when(cycleCountService.startCycleCount(id)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/cycle-counts/{id}/start", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void enterCount_ShouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        CycleCountEntryRequest entry = new CycleCountEntryRequest();
        entry.setProductVariantId(UUID.randomUUID());
        entry.setCountedQuantity(BigDecimal.TEN);
        List<CycleCountEntryRequest> entries = Collections.singletonList(entry);

        doNothing().when(cycleCountService).enterCount(eq(id), any());

        mockMvc.perform(post("/api/v1/cycle-counts/{id}/entries", id)
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entries)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void approveCycleCount_ShouldReturnCompleted() throws Exception {
        UUID id = UUID.randomUUID();
        CycleCountDto dto = new CycleCountDto();
        dto.setId(id);
        dto.setStatus(CycleCountStatus.COMPLETED);

        when(cycleCountService.approveCycleCount(id)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/cycle-counts/{id}/approve", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
