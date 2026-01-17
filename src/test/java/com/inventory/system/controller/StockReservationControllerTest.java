package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.ReservationPriority;
import com.inventory.system.common.entity.StockReservationStatus;
import com.inventory.system.payload.StockReservationDto;
import com.inventory.system.payload.StockReservationRequest;
import com.inventory.system.service.StockReservationService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StockReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockReservationService stockReservationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    public void reserveStock_ShouldReturnCreated() throws Exception {
        StockReservationRequest request = new StockReservationRequest();
        request.setProductVariantId(UUID.randomUUID());
        request.setWarehouseId(UUID.randomUUID());
        request.setQuantity(BigDecimal.TEN);

        StockReservationDto response = new StockReservationDto();
        response.setId(UUID.randomUUID());
        response.setQuantity(BigDecimal.TEN);
        response.setStatus(StockReservationStatus.ACTIVE);
        response.setPriority(ReservationPriority.MEDIUM);

        when(stockReservationService.reserveStock(any(StockReservationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/stock-reservations")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void releaseReservation_ShouldReturnOk() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(stockReservationService).releaseReservation(id);

        mockMvc.perform(put("/api/v1/stock-reservations/{id}/release", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getATP_ShouldReturnQuantity() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        BigDecimal atp = BigDecimal.valueOf(50);

        when(stockReservationService.getAvailableToPromise(variantId, warehouseId)).thenReturn(atp);

        mockMvc.perform(get("/api/v1/stock-reservations/atp")
                .param("productVariantId", variantId.toString())
                .param("warehouseId", warehouseId.toString())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(50));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getReservations_ShouldReturnPage() throws Exception {
        StockReservationDto dto = new StockReservationDto();
        dto.setId(UUID.randomUUID());
        dto.setStatus(StockReservationStatus.ACTIVE);
        Page<StockReservationDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(stockReservationService.getReservations(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/stock-reservations")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }
}
