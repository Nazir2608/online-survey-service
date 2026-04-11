package com.nazir.onlinesurveyservice.controller;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.dto.request.ChangePasswordRequest;
import com.nazir.onlinesurveyservice.dto.request.UpdateProfileRequest;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.UserResponse;
import com.nazir.onlinesurveyservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users", description = "User profile and admin management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─── Own profile ──────────────────────────────────────────────────────────

    @Operation(summary = "Get own profile")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @Operation(summary = "Update own profile")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @Operation(summary = "Change own password")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @Operation(summary = "[ADMIN] List all users")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(@RequestParam(defaultValue = "0")  int page, @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @Operation(summary = "[ADMIN] Get user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "[ADMIN] Change user role")
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(@PathVariable UUID id, @RequestParam Role role) {
        return ResponseEntity.ok(userService.changeRole(id, role));
    }

    @Operation(summary = "[ADMIN] Enable or disable a user")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable UUID id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }
}
