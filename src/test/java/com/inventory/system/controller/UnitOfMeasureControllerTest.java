package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.UnitOfMeasure.UomCategory;
import com.inventory.system.payload.UnitOfMeasureDto;
import com.inventory.system.service.UnitOfMeasureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
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
public class UnitOfMeasureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnitOfMeasureService uomService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createUom_ShouldReturnCreatedUom() throws Exception {
        UnitOfMeasureDto inputDto = new UnitOfMeasureDto();
        inputDto.setName("Kilogram");
        inputDto.setCode("kg");
        inputDto.setCategory(UomCategory.WEIGHT);
        inputDto.setIsBase(true);
        inputDto.setConversionFactor(BigDecimal.ONE);

        UnitOfMeasureDto outputDto = new UnitOfMeasureDto();
        outputDto.setId(UUID.randomUUID());
        outputDto.setName("Kilogram");
        outputDto.setCode("kg");
        outputDto.setCategory(UomCategory.WEIGHT);
        outputDto.setIsBase(true);
        outputDto.setConversionFactor(BigDecimal.ONE);

        when(uomService.createUom(any(UnitOfMeasureDto.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/uoms")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Kilogram"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void updateUom_ShouldReturnUpdatedUom() throws Exception {
        UUID id = UUID.randomUUID();
        UnitOfMeasureDto inputDto = new UnitOfMeasureDto();
        inputDto.setName("Gram");

        UnitOfMeasureDto outputDto = new UnitOfMeasureDto();
        outputDto.setId(id);
        outputDto.setName("Gram");

        when(uomService.updateUom(eq(id), any(UnitOfMeasureDto.class))).thenReturn(outputDto);

        mockMvc.perform(put("/api/v1/uoms/{id}", id)
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Gram"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteUom_ShouldReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(uomService).deleteUom(id);

        mockMvc.perform(delete("/api/v1/uoms/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void getUom_ShouldReturnUom() throws Exception {
        UUID id = UUID.randomUUID();
        UnitOfMeasureDto dto = new UnitOfMeasureDto();
        dto.setId(id);
        dto.setName("Liter");

        when(uomService.getUom(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/uoms/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Liter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllUoms_ShouldReturnList() throws Exception {
        UnitOfMeasureDto dto = new UnitOfMeasureDto();
        dto.setName("Meter");
        when(uomService.getAllUoms()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/uoms")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Meter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void convert_ShouldReturnResult() throws Exception {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        BigDecimal result = new BigDecimal("1000");

        when(uomService.convert(any(BigDecimal.class), eq(fromId), eq(toId))).thenReturn(result);

        mockMvc.perform(get("/api/v1/uoms/convert")
                .header("X-Tenant-ID", "test-tenant")
                .param("value", "1")
                .param("fromUomId", fromId.toString())
                .param("toUomId", toId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1000));
    }
}
