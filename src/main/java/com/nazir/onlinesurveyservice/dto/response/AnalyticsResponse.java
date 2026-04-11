package com.nazir.onlinesurveyservice.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AnalyticsResponse(
        UUID   surveyId,
        String surveyTitle,
        long   totalResponses,
        List<QuestionStats>  questionStats,
        List<DailyCount>     dailyTrend
) {
    public record QuestionStats(
            UUID   questionId,
            String questionText,
            String questionType,
            // For MCQ
            List<OptionCount> optionCounts,
            // For RATING/NUMBER
            Double averageValue,
            Integer minValue,
            Integer maxValue,
            // For TEXT/PARAGRAPH
            long   answerCount
    ) {}

    public record OptionCount(UUID optionId, String optionText, long count, double percentage) {}

    public record DailyCount(LocalDate date, long count) {}
}
