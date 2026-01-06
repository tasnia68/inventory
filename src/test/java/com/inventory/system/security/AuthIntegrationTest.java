package com.inventory.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.common.entity.User;
import com.inventory.system.payload.AuthResponse;
import com.inventory.system.payload.LoginRequest;
import com.inventory.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldAuthenticateAndGetToken() throws Exception {
        // Given
        com.inventory.system.config.tenant.TenantContext.setTenantId(TENANT_ID);
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("Test");
        user.setLastName("User");
        userRepository.save(user);
        com.inventory.system.config.tenant.TenantContext.clear();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldReturn401WhenAccessingProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/unknown")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isUnauthorized());
    }

    /*
    @Test
    void shouldReturn401WhenTenantDoesNotMatch() throws Exception {
        // Setup user in Tenant A
        com.inventory.system.config.tenant.TenantContext.setTenantId("tenant-A");
        User user = new User();
        user.setEmail("user@tenantA.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("First");
        user.setLastName("Last");
        userRepository.save(user);
        com.inventory.system.config.tenant.TenantContext.clear();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@tenantA.com");
        loginRequest.setPassword("password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Tenant-ID", "tenant-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    */
}
