package com.nazir.onlinesurveyservice.dto.response;

import com.nazir.onlinesurveyservice.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID    id,
        String  email,
        String  displayName,
        Role    role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
