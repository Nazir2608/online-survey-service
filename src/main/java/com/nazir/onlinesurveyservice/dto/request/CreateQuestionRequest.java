package com.nazir.onlinesurveyservice.dto.request;

import com.nazir.onlinesurveyservice.domain.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionRequest(

        @NotBlank(message = "Question text is required")
        @Size(max = 1000, message = "Question text must be at most 1000 characters")
        String text,

        @Size(max = 500, message = "Help text must be at most 500 characters")
        String helpText,

        @NotNull(message = "Question type is required")
        QuestionType type,

        boolean required,

        Integer minValue,

        Integer maxValue,

        @Valid
        List<CreateOptionRequest> options
) {}
