package com.nazir.onlinesurveyservice.service.impl;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.dto.request.ChangePasswordRequest;
import com.nazir.onlinesurveyservice.dto.request.UpdateProfileRequest;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.UserResponse;
import com.nazir.onlinesurveyservice.exception.ResourceNotFoundException;
import com.nazir.onlinesurveyservice.repository.UserRepository;
import com.nazir.onlinesurveyservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getProfile(User currentUser) {
        return toResponse(currentUser);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        currentUser.setDisplayName(request.displayName());
        return toResponse(userRepository.save(currentUser));
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable), this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse changeRole(UUID id, Role role) {
        User user = findUserOrThrow(id);
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse setEnabled(UUID id, boolean enabled) {
        User user = findUserOrThrow(id);
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
