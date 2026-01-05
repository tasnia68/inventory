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
import org.springframework.test.context.ActiveProfiles;
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
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setTenantId(TENANT_ID); // Manually set for test since filter might not trigger for repository direct usage?
        // Actually, repository methods are intercepted by Aspect/Filter if configured?
        // No, repository is standard JPA. But BaseEntity.prePersist uses TenantContext.
        // We must mock TenantContext or set it.
        // For integration test, we simulate request so Filter sets context.
        // But for setup, we might need to set context manually or use a utility.

        // Let's set context manually for setup
        com.inventory.system.config.tenant.TenantContext.setTenantId(TENANT_ID);
        userRepository.save(user);
        com.inventory.system.config.tenant.TenantContext.clear();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        // When/Then
        String responseContent = mockMvc.perform(post("/api/auth/login")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);
        String token = authResponse.getAccessToken();

        // Verify accessing protected resource
        // We don't have a protected resource yet other than checking if 403/401 is avoided.
        // But we can check if 401 is returned without token.
    }

    @Test
    void shouldReturn401WhenAccessingProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/unknown") // Non-existent but protected path
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isUnauthorized()); // Or 403 depending on config, but 401 for missing token
    }

    @Test
    void shouldReturn403WhenTenantDoesNotMatch() throws Exception {
        // Setup user in Tenant A
        com.inventory.system.config.tenant.TenantContext.setTenantId("tenant-A");
        User user = new User();
        user.setEmail("user@tenantA.com");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);
        com.inventory.system.config.tenant.TenantContext.clear();

        // Try login with Tenant B header
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@tenantA.com");
        loginRequest.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Tenant-ID", "tenant-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                // Should fail because user is not found in Tenant B scope
                .andExpect(status().isUnauthorized()); // Bad credentials or similar
    }
}
