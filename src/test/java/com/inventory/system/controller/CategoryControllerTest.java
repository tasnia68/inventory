package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.CategoryDto;
import com.inventory.system.service.CategoryService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCategory_ShouldReturnCreatedCategory() throws Exception {
        CategoryDto inputDto = new CategoryDto();
        inputDto.setName("Electronics");
        inputDto.setDescription("Electronic items");

        CategoryDto outputDto = new CategoryDto();
        outputDto.setId(UUID.randomUUID());
        outputDto.setName("Electronics");
        outputDto.setDescription("Electronic items");

        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(outputDto);

        mockMvc.perform(post("/api/v1/categories")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void updateCategory_ShouldReturnUpdatedCategory() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryDto inputDto = new CategoryDto();
        inputDto.setName("Updated Electronics");

        CategoryDto outputDto = new CategoryDto();
        outputDto.setId(id);
        outputDto.setName("Updated Electronics");

        when(categoryService.updateCategory(eq(id), any(CategoryDto.class))).thenReturn(outputDto);

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteCategory_ShouldReturnSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(categoryService).deleteCategory(id);

        mockMvc.perform(delete("/api/v1/categories/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void getCategory_ShouldReturnCategory() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryDto dto = new CategoryDto();
        dto.setId(id);
        dto.setName("Books");

        when(categoryService.getCategory(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/categories/{id}", id)
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Books"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllCategories_ShouldReturnList() throws Exception {
        CategoryDto dto = new CategoryDto();
        dto.setName("Furniture");
        List<CategoryDto> list = Collections.singletonList(dto);

        when(categoryService.getAllCategories()).thenReturn(list);

        mockMvc.perform(get("/api/v1/categories")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Furniture"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getCategoryTree_ShouldReturnTree() throws Exception {
        CategoryDto parent = new CategoryDto();
        parent.setName("Parent");
        CategoryDto child = new CategoryDto();
        child.setName("Child");
        parent.setChildren(Collections.singletonList(child));
        List<CategoryDto> tree = Collections.singletonList(parent);

        when(categoryService.getCategoryTree()).thenReturn(tree);

        mockMvc.perform(get("/api/v1/categories/tree")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Parent"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("Child"));
    }
}
