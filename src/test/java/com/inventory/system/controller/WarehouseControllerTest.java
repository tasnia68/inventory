package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.CreateWarehouseRequest;
import com.inventory.system.payload.UpdateWarehouseRequest;
import com.inventory.system.payload.WarehouseDto;
import com.inventory.system.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarehouseService warehouseService;

    @Autowired
    private ObjectMapper objectMapper;

    private WarehouseDto warehouseDto;

    @BeforeEach
    void setUp() {
        warehouseDto = new WarehouseDto();
        warehouseDto.setId(UUID.randomUUID());
        warehouseDto.setName("Main Warehouse");
        warehouseDto.setLocation("New York");
        warehouseDto.setType("DISTRIBUTION");
        warehouseDto.setIsActive(true);
    }

    @Test
    @WithMockUser
    void createWarehouse() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest();
        request.setName("Main Warehouse");
        request.setLocation("New York");
        request.setType("DISTRIBUTION");

        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class))).thenReturn(warehouseDto);

        mockMvc.perform(post("/api/v1/warehouses")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Warehouse"));
    }

    @Test
    @WithMockUser
    void getWarehouseById() throws Exception {
        when(warehouseService.getWarehouseById(warehouseDto.getId())).thenReturn(warehouseDto);

        mockMvc.perform(get("/api/v1/warehouses/{id}", warehouseDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(warehouseDto.getId().toString()));
    }

    @Test
    @WithMockUser
    void getAllWarehouses() throws Exception {
        when(warehouseService.getAllWarehouses()).thenReturn(Collections.singletonList(warehouseDto));

        mockMvc.perform(get("/api/v1/warehouses")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Main Warehouse"));
    }

    @Test
    @WithMockUser
    void updateWarehouse() throws Exception {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest();
        request.setName("Updated Warehouse");

        warehouseDto.setName("Updated Warehouse");
        when(warehouseService.updateWarehouse(eq(warehouseDto.getId()), any(UpdateWarehouseRequest.class))).thenReturn(warehouseDto);

        mockMvc.perform(put("/api/v1/warehouses/{id}", warehouseDto.getId())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Warehouse"));
    }

    @Test
    @WithMockUser
    void deleteWarehouse() throws Exception {
        mockMvc.perform(delete("/api/v1/warehouses/{id}", warehouseDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
