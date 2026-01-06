package com.inventory.system.service;

import com.inventory.system.common.entity.User;
import com.inventory.system.payload.AcceptInvitationRequest;
import com.inventory.system.payload.CreateUserRequest;
import com.inventory.system.payload.UpdateUserRequest;
import com.inventory.system.payload.UserDto;
import com.inventory.system.payload.UserInvitationRequest;
import com.inventory.system.payload.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    void inviteUser(UserInvitationRequest request);
    void acceptInvitation(AcceptInvitationRequest request);
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
    UserDto getUser(UUID id);
    List<UserDto> getAllUsers();
    UserProfileResponse getCurrentUserProfile();
    UserProfileResponse updateCurrentUserProfile(UpdateUserRequest request);
}
