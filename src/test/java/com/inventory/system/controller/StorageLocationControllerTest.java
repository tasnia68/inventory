package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.StorageLocation.StorageLocationType;
import com.inventory.system.payload.CreateStorageLocationRequest;
import com.inventory.system.payload.StorageLocationDto;
import com.inventory.system.service.StorageLocationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StorageLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageLocationService storageLocationService;

    @Autowired
    private ObjectMapper objectMapper;

    private StorageLocationDto storageLocationDto;
    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        storageLocationDto = new StorageLocationDto();
        storageLocationDto.setId(UUID.randomUUID());
        storageLocationDto.setName("Zone A");
        storageLocationDto.setType(StorageLocationType.ZONE);
        storageLocationDto.setWarehouseId(warehouseId);
        storageLocationDto.setWarehouseName("Main Warehouse");
    }

    @Test
    @WithMockUser
    void createStorageLocation() throws Exception {
        CreateStorageLocationRequest request = new CreateStorageLocationRequest();
        request.setName("Zone A");
        request.setType(StorageLocationType.ZONE);
        request.setWarehouseId(warehouseId);

        when(storageLocationService.createStorageLocation(any(CreateStorageLocationRequest.class))).thenReturn(storageLocationDto);

        mockMvc.perform(post("/api/v1/storage-locations")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Zone A"));
    }

    @Test
    @WithMockUser
    void getStorageLocationById() throws Exception {
        when(storageLocationService.getStorageLocationById(storageLocationDto.getId())).thenReturn(storageLocationDto);

        mockMvc.perform(get("/api/v1/storage-locations/{id}", storageLocationDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(storageLocationDto.getId().toString()));
    }

    @Test
    @WithMockUser
    void getStorageLocationsByWarehouse() throws Exception {
        when(storageLocationService.getStorageLocationsByWarehouse(warehouseId)).thenReturn(Collections.singletonList(storageLocationDto));

        mockMvc.perform(get("/api/v1/storage-locations")
                .header("X-Tenant-ID", "test-tenant")
                .param("warehouseId", warehouseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Zone A"));
    }

    @Test
    @WithMockUser
    void deleteStorageLocation() throws Exception {
        mockMvc.perform(delete("/api/v1/storage-locations/{id}", storageLocationDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
