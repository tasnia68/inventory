package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.StockTransactionStatus;
import com.inventory.system.common.entity.StockTransactionType;
import com.inventory.system.payload.CreateStockTransactionRequest;
import com.inventory.system.payload.StockTransactionDto;
import com.inventory.system.service.StockTransactionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StockTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockTransactionService stockTransactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void createTransaction_ShouldReturnCreated() throws Exception {
        CreateStockTransactionRequest request = new CreateStockTransactionRequest();
        request.setType(StockTransactionType.INBOUND);
        request.setDestinationWarehouseId(UUID.randomUUID());
        CreateStockTransactionRequest.ItemRequest item = new CreateStockTransactionRequest.ItemRequest();
        item.setProductVariantId(UUID.randomUUID());
        item.setQuantity(BigDecimal.TEN);
        request.setItems(Collections.singletonList(item));

        StockTransactionDto responseDto = new StockTransactionDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setStatus(StockTransactionStatus.DRAFT);
        responseDto.setType(StockTransactionType.INBOUND);

        when(stockTransactionService.createTransaction(any(CreateStockTransactionRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/stock-transactions")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }
}
