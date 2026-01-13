package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.ProductAttribute.AttributeType;
import com.inventory.system.payload.AttributeGroupDto;
import com.inventory.system.payload.ProductAttributeDto;
import com.inventory.system.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductAttributeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Attribute Group Tests ---

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createAttributeGroup_ShouldReturnCreated() throws Exception {
        AttributeGroupDto input = new AttributeGroupDto();
        input.setName("General");

        AttributeGroupDto output = new AttributeGroupDto();
        output.setId(UUID.randomUUID());
        output.setName("General");

        when(productService.createAttributeGroup(any(AttributeGroupDto.class))).thenReturn(output);

        mockMvc.perform(post("/api/v1/attribute-groups")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("General"));
    }

    // --- Product Attribute Tests ---

    @Test
    @WithMockUser(roles = "MANAGER")
    public void createProductAttribute_ShouldReturnCreated() throws Exception {
        ProductAttributeDto input = new ProductAttributeDto();
        input.setName("Color");
        input.setType(AttributeType.TEXT);

        ProductAttributeDto output = new ProductAttributeDto();
        output.setId(UUID.randomUUID());
        output.setName("Color");
        output.setType(AttributeType.TEXT);

        when(productService.createProductAttribute(any(ProductAttributeDto.class))).thenReturn(output);

        mockMvc.perform(post("/api/v1/product-attributes")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Color"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void getAttributesByTemplate_ShouldReturnList() throws Exception {
        UUID templateId = UUID.randomUUID();
        ProductAttributeDto dto = new ProductAttributeDto();
        dto.setName("Size");

        when(productService.getAttributesByTemplate(templateId)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/product-attributes")
                .header("X-Tenant-ID", "test-tenant")
                .param("templateId", templateId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Size"));
    }
}
