package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.CreateSupplierProductRequest;
import com.inventory.system.payload.SupplierProductDto;
import com.inventory.system.payload.UpdateSupplierProductRequest;
import com.inventory.system.service.SupplierProductService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SupplierProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierProductService supplierProductService;

    @Autowired
    private ObjectMapper objectMapper;

    private SupplierProductDto supplierProductDto;
    private UUID supplierId;
    private UUID productVariantId;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
        productVariantId = UUID.randomUUID();

        supplierProductDto = new SupplierProductDto();
        supplierProductDto.setId(UUID.randomUUID());
        supplierProductDto.setSupplierId(supplierId);
        supplierProductDto.setSupplierName("Test Supplier");
        supplierProductDto.setProductVariantId(productVariantId);
        supplierProductDto.setProductVariantName("SKU-123");
        supplierProductDto.setSupplierSku("SUP-SKU-001");
        supplierProductDto.setPrice(new BigDecimal("100.00"));
        supplierProductDto.setCurrency("USD");
        supplierProductDto.setLeadTimeDays(5);
    }

    @Test
    @WithMockUser
    void addProductToSupplier() throws Exception {
        CreateSupplierProductRequest request = new CreateSupplierProductRequest();
        request.setSupplierId(supplierId);
        request.setProductVariantId(productVariantId);
        request.setSupplierSku("SUP-SKU-001");
        request.setPrice(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setLeadTimeDays(5);

        when(supplierProductService.createSupplierProduct(any(CreateSupplierProductRequest.class))).thenReturn(supplierProductDto);

        mockMvc.perform(post("/api/v1/suppliers/{supplierId}/products", supplierId)
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supplierSku").value("SUP-SKU-001"));
    }

    @Test
    @WithMockUser
    void getProductsBySupplier() throws Exception {
        when(supplierProductService.getProductsBySupplier(supplierId)).thenReturn(Collections.singletonList(supplierProductDto));

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}/products", supplierId)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].supplierSku").value("SUP-SKU-001"));
    }

    @Test
    @WithMockUser
    void updateSupplierProduct() throws Exception {
        UpdateSupplierProductRequest request = new UpdateSupplierProductRequest();
        request.setPrice(new BigDecimal("120.00"));

        supplierProductDto.setPrice(new BigDecimal("120.00"));
        when(supplierProductService.updateSupplierProduct(eq(supplierProductDto.getId()), any(UpdateSupplierProductRequest.class)))
                .thenReturn(supplierProductDto);

        mockMvc.perform(put("/api/v1/suppliers/products/{id}", supplierProductDto.getId())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.price").value(120.00));
    }

    @Test
    @WithMockUser
    void removeProductFromSupplier() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/products/{id}", supplierProductDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
