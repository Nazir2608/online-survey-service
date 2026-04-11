package com.nazir.onlinesurveyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateSurveyRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be 3–255 characters")
        String title,

        @Size(max = 2000, message = "Description can be at most 2000 characters")
        String description,

        boolean anonymous,

        Integer maxResponses,

        Instant startDate,

        Instant endDate
) {}
