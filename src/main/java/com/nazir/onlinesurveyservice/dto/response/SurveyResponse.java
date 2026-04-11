package com.nazir.onlinesurveyservice.dto.response;

import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveyResponse(
        UUID         id,
        String       title,
        String       description,
        String       slug,
        SurveyStatus status,
        boolean      anonymous,
        Integer      maxResponses,
        Instant      startDate,
        Instant      endDate,
        CreatorSummary creator,
        int          questionCount,
        Instant      createdAt,
        Instant      updatedAt
) {
    public record CreatorSummary(UUID id, String displayName) {}
}
