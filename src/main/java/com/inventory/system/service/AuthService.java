package com.inventory.system.service;

import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.entity.User;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.AuthResponse;
import com.inventory.system.payload.LoginRequest;
import com.inventory.system.repository.TenantRepository;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(UserDetailsService userDetailsService,
                       JwtService jwtService,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public AuthResponse authenticate(LoginRequest request) {
        Tenant tenant = resolveTenant(request.getWorkspace());
        String tenantId = tenant.getId().toString();
        String tenantSubdomain = tenant.getSubdomain();

        TenantContext.setTenantId(tenantId);
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                throw new BadCredentialsException("Bad credentials");
            }

            String accessToken = jwtService.generateToken(userDetails, TenantContext.getTenantId());
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            boolean mustChangePassword = userRepository
                    .findByEmailAndTenantId(request.getEmail(), tenantId)
                    .map(User::isForcePasswordChange)
                    .orElse(false);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tenantId(tenantId)
                    .tenantSubdomain(tenantSubdomain)
                    .mustChangePassword(mustChangePassword)
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    private Tenant resolveTenant(String workspace) {
        if (workspace == null || workspace.isBlank()) {
            throw new BadRequestException("Workspace is required");
        }

        String normalized = workspace.trim().toLowerCase();
        return tenantRepository.findBySubdomainIgnoreCase(normalized)
                .or(() -> {
                    try {
                        return tenantRepository.findById(UUID.fromString(normalized));
                    } catch (IllegalArgumentException exception) {
                        return java.util.Optional.empty();
                    }
                })
                .orElseThrow(() -> new BadRequestException("Unknown workspace: " + workspace.trim()));
    }
}
