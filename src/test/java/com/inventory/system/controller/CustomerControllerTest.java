package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.CustomerStatus;
import com.inventory.system.payload.CreateCustomerRequest;
import com.inventory.system.payload.CustomerDto;
import com.inventory.system.payload.UpdateCustomerRequest;
import com.inventory.system.service.CustomerService;
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
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        customerDto = new CustomerDto();
        customerDto.setId(UUID.randomUUID());
        customerDto.setName("Test Customer");
        customerDto.setContactName("Jane Doe");
        customerDto.setEmail("jane.doe@example.com");
        customerDto.setPhoneNumber("987-654-3210");
        customerDto.setAddress("456 Test Ave");
        customerDto.setCreditLimit(new BigDecimal("1000.00"));
        customerDto.setIsActive(true);
        customerDto.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    @WithMockUser
    void createCustomer() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Test Customer");
        request.setContactName("Jane Doe");
        request.setEmail("jane.doe@example.com");
        request.setPhoneNumber("987-654-3210");
        request.setAddress("456 Test Ave");
        request.setCreditLimit(new BigDecimal("1000.00"));
        request.setIsActive(true);
        request.setStatus(CustomerStatus.ACTIVE);

        when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(customerDto);

        mockMvc.perform(post("/api/v1/customers")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Customer"));
    }

    @Test
    @WithMockUser
    void getCustomerById() throws Exception {
        when(customerService.getCustomerById(customerDto.getId())).thenReturn(customerDto);

        mockMvc.perform(get("/api/v1/customers/{id}", customerDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(customerDto.getId().toString()));
    }

    @Test
    @WithMockUser
    void getAllCustomers() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(Collections.singletonList(customerDto));

        mockMvc.perform(get("/api/v1/customers")
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Test Customer"));
    }

    @Test
    @WithMockUser
    void updateCustomer() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName("Updated Customer");

        customerDto.setName("Updated Customer");
        when(customerService.updateCustomer(eq(customerDto.getId()), any(UpdateCustomerRequest.class))).thenReturn(customerDto);

        mockMvc.perform(put("/api/v1/customers/{id}", customerDto.getId())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Customer"));
    }

    @Test
    @WithMockUser
    void deleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/{id}", customerDto.getId())
                .header("X-Tenant-ID", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
