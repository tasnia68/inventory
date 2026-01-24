package com.inventory.system.controller;

import com.inventory.system.payload.AuthResponse;
import com.inventory.system.payload.LoginRequest;
import com.inventory.system.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/test")
    public String test() {
        log.info("===== TEST ENDPOINT HIT =====");
        return "Auth controller is working!";
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("===== LOGIN REQUEST RECEIVED =====");
        log.info("Email: {}", request.getEmail());
        log.info("Password present: {}", request.getPassword() != null);
        AuthResponse response = authService.authenticate(request);
        log.info("===== LOGIN SUCCESSFUL =====");
        return ResponseEntity.ok(response);
    }
}
