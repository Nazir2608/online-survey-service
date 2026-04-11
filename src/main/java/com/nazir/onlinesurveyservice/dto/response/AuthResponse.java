package com.nazir.onlinesurveyservice.dto.response;

import com.nazir.onlinesurveyservice.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String  accessToken,
        String  refreshToken,
        String  tokenType,
        long    expiresIn,      // seconds
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String displayName, Role role) {}

    public static String BEARER = "Bearer";
}
