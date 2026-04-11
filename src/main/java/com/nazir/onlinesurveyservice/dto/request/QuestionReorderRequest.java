package com.nazir.onlinesurveyservice.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record QuestionReorderRequest(

        @NotEmpty(message = "Question ID list must not be empty")
        List<UUID> questionIds   // ordered list — first element gets orderIndex 0
) {}
