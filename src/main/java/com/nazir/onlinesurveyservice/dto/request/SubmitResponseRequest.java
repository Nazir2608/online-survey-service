package com.nazir.onlinesurveyservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SubmitResponseRequest(

        @NotEmpty(message = "Answers list must not be empty")
        @Valid
        List<AnswerRequest> answers
) {
    public record AnswerRequest(

            @NotNull(message = "Question ID is required")
            UUID questionId,

            // Only one of the following should be populated — validated in service
            String textValue,

            Integer ratingValue,

            Set<UUID> selectedOptionIds
    ) {}
}
