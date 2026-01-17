package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.StockMovement;
import com.inventory.system.payload.StockAdjustmentDto;
import com.inventory.system.payload.StockDto;
import com.inventory.system.payload.StockMovementDto;
import com.inventory.system.service.StockService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    public void getStocks_ShouldReturnPage() throws Exception {
        StockDto dto = new StockDto();
        dto.setId(UUID.randomUUID());
        dto.setQuantity(BigDecimal.TEN);
        Page<StockDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(stockService.getStocks(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/stocks")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].quantity").value(10));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void adjustStock_ShouldReturnStock() throws Exception {
        StockAdjustmentDto inputDto = new StockAdjustmentDto();
        inputDto.setProductVariantId(UUID.randomUUID());
        inputDto.setWarehouseId(UUID.randomUUID());
        inputDto.setQuantity(BigDecimal.valueOf(5));
        inputDto.setType(StockMovement.StockMovementType.IN);

        StockMovementDto outputDto = new StockMovementDto();
        outputDto.setId(UUID.randomUUID());
        outputDto.setQuantity(BigDecimal.valueOf(5));
        outputDto.setType(StockMovement.StockMovementType.IN);

        when(stockService.adjustStock(any(StockAdjustmentDto.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/stocks/adjust")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getStockMovements_ShouldReturnPage() throws Exception {
        StockMovementDto dto = new StockMovementDto();
        dto.setType(StockMovement.StockMovementType.IN);
        dto.setQuantity(BigDecimal.TEN);
        Page<StockMovementDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(stockService.getStockMovements(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/stocks/movements")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("IN"));
    }
}
