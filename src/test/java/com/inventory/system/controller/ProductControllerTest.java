package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.ProductSearchDto;
import com.inventory.system.payload.ProductVariantDto;
import com.inventory.system.service.ProductService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    public void createProduct_ShouldReturnCreatedProduct() throws Exception {
        ProductVariantDto inputDto = new ProductVariantDto();
        inputDto.setTemplateId(UUID.randomUUID());
        inputDto.setPrice(BigDecimal.valueOf(100.00));

        ProductVariantDto outputDto = new ProductVariantDto();
        outputDto.setId(UUID.randomUUID());
        outputDto.setSku("TEST-SKU-123");
        outputDto.setPrice(BigDecimal.valueOf(100.00));

        when(productService.createProductVariant(any(ProductVariantDto.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/products")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("TEST-SKU-123"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void getProduct_ShouldReturnProduct() throws Exception {
        UUID id = UUID.randomUUID();
        ProductVariantDto dto = new ProductVariantDto();
        dto.setId(id);
        dto.setSku("TEST-SKU-123");

        when(productService.getProductVariant(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/products/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("TEST-SKU-123"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllProducts_ShouldReturnPage() throws Exception {
        ProductVariantDto dto = new ProductVariantDto();
        dto.setSku("TEST-SKU-123");
        Page<ProductVariantDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(productService.getAllProductVariants(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("TEST-SKU-123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteProduct_ShouldReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void searchProducts_ShouldReturnFilteredResults() throws Exception {
        ProductSearchDto searchDto = new ProductSearchDto();
        searchDto.setSku("TEST-SKU-123");

        ProductVariantDto dto = new ProductVariantDto();
        dto.setSku("TEST-SKU-123");
        Page<ProductVariantDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(productService.searchProducts(any(ProductSearchDto.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(post("/api/v1/products/search")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("TEST-SKU-123"));
    }
}
