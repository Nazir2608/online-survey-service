package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.dto.request.ChangePasswordRequest;
import com.nazir.onlinesurveyservice.dto.request.UpdateProfileRequest;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getProfile(User currentUser);

    UserResponse updateProfile(User currentUser, UpdateProfileRequest request);

    void changePassword(User currentUser, ChangePasswordRequest request);

    // Admin
    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(UUID id);

    UserResponse changeRole(UUID id, Role role);

    UserResponse setEnabled(UUID id, boolean enabled);
}
