package com.nazir.onlinesurveyservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ResponseSummaryResponse(
        UUID    id,
        UUID    surveyId,
        RespondentSummary respondent,   // null for anonymous
        Instant submittedAt
) {
    public record RespondentSummary(UUID id, String displayName) {}
}
