package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.ProductTemplateDto;
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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createTemplate_ShouldReturnCreatedTemplate() throws Exception {
        ProductTemplateDto inputDto = new ProductTemplateDto();
        inputDto.setName("Electronics");
        inputDto.setDescription("Electronic items");

        ProductTemplateDto outputDto = new ProductTemplateDto();
        outputDto.setId(UUID.randomUUID());
        outputDto.setName("Electronics");
        outputDto.setDescription("Electronic items");
        outputDto.setIsActive(true);

        when(productService.createTemplate(any(ProductTemplateDto.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/product-templates")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getTemplate_ShouldReturnTemplate() throws Exception {
        UUID id = UUID.randomUUID();
        ProductTemplateDto dto = new ProductTemplateDto();
        dto.setId(id);
        dto.setName("Clothing");

        when(productService.getTemplate(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/product-templates/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Clothing"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void updateTemplate_ShouldReturnUpdatedTemplate() throws Exception {
        UUID id = UUID.randomUUID();
        ProductTemplateDto inputDto = new ProductTemplateDto();
        inputDto.setName("Updated Name");

        ProductTemplateDto outputDto = new ProductTemplateDto();
        outputDto.setId(id);
        outputDto.setName("Updated Name");

        when(productService.updateTemplate(eq(id), any(ProductTemplateDto.class))).thenReturn(outputDto);

        mockMvc.perform(put("/api/v1/product-templates/{id}", id)
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteTemplate_ShouldReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/product-templates/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void getAllTemplates_ShouldReturnPage() throws Exception {
        ProductTemplateDto dto = new ProductTemplateDto();
        dto.setName("Furniture");
        Page<ProductTemplateDto> page = new PageImpl<>(Collections.singletonList(dto));

        when(productService.getAllTemplates(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/product-templates")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Furniture"));
    }
}
