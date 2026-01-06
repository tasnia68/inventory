package com.inventory.system.controller;

import com.inventory.system.payload.AcceptInvitationRequest;
import com.inventory.system.payload.ApiResponse;
import com.inventory.system.payload.CreateUserRequest;
import com.inventory.system.payload.UpdateUserRequest;
import com.inventory.system.payload.UserDto;
import com.inventory.system.payload.UserInvitationRequest;
import com.inventory.system.payload.UserProfileResponse;
import com.inventory.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> inviteUser(@Valid @RequestBody UserInvitationRequest request) {
        userService.inviteUser(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Invitation sent successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/invite/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        userService.acceptInvitation(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Invitation accepted successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserDto>builder()
                .status(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(user)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.<List<UserDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Users retrieved successfully")
                .data(users)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable UUID id) {
        UserDto user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .status(HttpStatus.OK.value())
                .message("User retrieved successfully")
                .data(user)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.<UserDto>builder()
                .status(HttpStatus.OK.value())
                .message("User updated successfully")
                .data(user)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("User deleted successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Profile retrieved successfully")
                .data(profile)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(@RequestBody UpdateUserRequest request) {
        UserProfileResponse profile = userService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Profile updated successfully")
                .data(profile)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
