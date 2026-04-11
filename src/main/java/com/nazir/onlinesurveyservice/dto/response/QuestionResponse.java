package com.nazir.onlinesurveyservice.dto.response;

import com.nazir.onlinesurveyservice.domain.enums.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID         id,
        String       text,
        String       helpText,
        QuestionType type,
        int          orderIndex,
        boolean      required,
        Integer      minValue,
        Integer      maxValue,
        List<OptionResponse> options,
        Instant      createdAt,
        Instant      updatedAt
) {}
