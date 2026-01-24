package com.inventory.system.service;

import com.inventory.system.common.entity.Role;
import com.inventory.system.common.entity.User;
import com.inventory.system.common.entity.UserActivityLog;
import com.inventory.system.common.entity.UserInvitation;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.payload.AcceptInvitationRequest;
import com.inventory.system.payload.CreateUserRequest;
import com.inventory.system.payload.UpdateUserRequest;
import com.inventory.system.payload.UserDto;
import com.inventory.system.payload.UserInvitationRequest;
import com.inventory.system.payload.UserProfileResponse;
import com.inventory.system.repository.RoleRepository;
import com.inventory.system.repository.UserActivityLogRepository;
import com.inventory.system.repository.UserInvitationRepository;
import com.inventory.system.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
            RoleRepository roleRepository,
            UserInvitationRepository userInvitationRepository,
            UserActivityLogRepository userActivityLogRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userInvitationRepository = userInvitationRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void inviteUser(UserInvitationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists.");
        }

        userInvitationRepository.findByEmail(request.getEmail()).ifPresent(existingInvitation -> {
            if (existingInvitation.getStatus() == UserInvitation.InvitationStatus.PENDING
                    && existingInvitation.getExpiryDate().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Active invitation already exists for this email.");
            }
            // If expired or accepted (unlikely if user not found), we delete/archive it or
            // just overwrite logic essentially by creating new one below?
            // Since token is unique, we should probably delete the old one or update it.
            // For simplicity, let's delete the old one.
            userInvitationRepository.delete(existingInvitation);
        });

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRoleName()));

        UserInvitation invitation = new UserInvitation();
        invitation.setEmail(request.getEmail());
        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(UserInvitation.InvitationStatus.PENDING);
        invitation.setExpiryDate(LocalDateTime.now().plusDays(7));

        userInvitationRepository.save(invitation);

        // Ideally, generate a full URL based on configuration
        String invitationLink = "/accept-invitation?token=" + invitation.getToken();
        emailService.sendInvitationEmail(request.getEmail(), invitationLink);

        logActivity("INVITE_USER", "Invited user: " + request.getEmail());
    }

    @Override
    public void acceptInvitation(AcceptInvitationRequest request) {
        UserInvitation invitation = userInvitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (invitation.getStatus() != UserInvitation.InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        if (invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitation.setStatus(UserInvitation.InvitationStatus.EXPIRED);
            userInvitationRepository.save(invitation);
            throw new RuntimeException("Invitation expired");
        }

        User user = new User();
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        roles.add(invitation.getRole());
        user.setRoles(roles);

        userRepository.save(user);

        invitation.setStatus(UserInvitation.InvitationStatus.ACCEPTED);
        userInvitationRepository.save(invitation);

        // Cannot log activity here easily as user is not logged in yet, or log as
        // system
    }

    @Override
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        logActivity("CREATE_USER", "Created user: " + savedUser.getEmail());
        return mapToDto(savedUser);
    }

    @Override
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getEnabled() != null)
            user.setEnabled(request.getEnabled());

        if (request.getRoles() != null) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        logActivity("UPDATE_USER", "Updated user: " + updatedUser.getId());
        return mapToDto(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.delete(user);
        logActivity("DELETE_USER", "Deleted user: " + id);
    }

    @Override
    public UserDto getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.inventory.system.common.entity.Permission::getName)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(permissions)
                .build();
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());

        User updatedUser = userRepository.save(user);
        logActivity("UPDATE_PROFILE", "User updated their profile");

        Set<String> permissions = updatedUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.inventory.system.common.entity.Permission::getName)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .createdAt(updatedUser.getCreatedAt())
                .roles(updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .permissions(permissions)
                .build();
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }

    private void logActivity(String action, String details) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email).orElse(null);

            UserActivityLog log = new UserActivityLog();
            log.setAction(action);
            log.setDetails(details + " | By: " + email);
            if (user != null) {
                log.setUserId(user.getId().toString());
            }
            // IP address would need to be passed down or retrieved from
            // RequestContextHolder if available
            // For now leaving IP null or mocking
            userActivityLogRepository.save(log);
        } catch (Exception e) {
            // Do not fail operation if logging fails
        }
    }
}
