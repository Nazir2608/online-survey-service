package com.nazir.onlinesurveyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 100, message = "Display name must be 2–100 characters")
        String displayName
) {}
