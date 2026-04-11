package com.nazir.onlinesurveyservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOptionRequest(

        @NotBlank(message = "Option text is required")
        @Size(max = 500, message = "Option text must be at most 500 characters")
        String text
) {}
