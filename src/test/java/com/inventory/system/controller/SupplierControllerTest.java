package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.CreateSupplierRequest;
import com.inventory.system.payload.SupplierDto;
import com.inventory.system.payload.UpdateSupplierRequest;
import com.inventory.system.service.SupplierService;
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
public class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierService supplierService;

    @Autowired
    private ObjectMapper objectMapper;

    private SupplierDto supplierDto;

    @BeforeEach
    void setUp() {
        supplierDto = new SupplierDto();
        supplierDto.setId(UUID.randomUUID());
        supplierDto.setName("Test Supplier");
        supplierDto.setContactName("John Doe");
        supplierDto.setEmail("john.doe@example.com");
        supplierDto.setPhoneNumber("123-456-7890");
        supplierDto.setAddress("123 Test St");
        supplierDto.setPaymentTerms("Net 30");
        supplierDto.setIsActive(true);
    }

    @Test
    @WithMockUser
    void createSupplier() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName("Test Supplier");
        request.setContactName("John Doe");
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("123-456-7890");
        request.setAddress("123 Test St");
        request.setPaymentTerms("Net 30");
        request.setIsActive(true);

        when(supplierService.createSupplier(any(CreateSupplierRequest.class))).thenReturn(supplierDto);

        mockMvc.perform(post("/api/v1/suppliers")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Supplier"));
    }

    @Test
    @WithMockUser
    void getSupplierById() throws Exception {
        when(supplierService.getSupplierById(supplierDto.getId())).thenReturn(supplierDto);

        mockMvc.perform(get("/api/v1/suppliers/{id}", supplierDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(supplierDto.getId().toString()));
    }

    @Test
    @WithMockUser
    void getAllSuppliers() throws Exception {
        when(supplierService.getAllSuppliers()).thenReturn(Collections.singletonList(supplierDto));

        mockMvc.perform(get("/api/v1/suppliers")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Supplier"));
    }

    @Test
    @WithMockUser
    void updateSupplier() throws Exception {
        UpdateSupplierRequest request = new UpdateSupplierRequest();
        request.setName("Updated Supplier");

        supplierDto.setName("Updated Supplier");
        when(supplierService.updateSupplier(eq(supplierDto.getId()), any(UpdateSupplierRequest.class))).thenReturn(supplierDto);

        mockMvc.perform(put("/api/v1/suppliers/{id}", supplierDto.getId())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Supplier"));
    }

    @Test
    @WithMockUser
    void deleteSupplier() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/{id}", supplierDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
