package com.nazir.onlinesurveyservice.dto.response;

import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full survey with all questions (used for preview / taking the survey) */
public record SurveyDetailResponse(
        UUID         id,
        String       title,
        String       description,
        String       slug,
        SurveyStatus status,
        boolean      anonymous,
        Integer      maxResponses,
        Instant      startDate,
        Instant      endDate,
        SurveyResponse.CreatorSummary creator,
        List<QuestionResponse> questions,
        Instant      createdAt,
        Instant      updatedAt
) {}
