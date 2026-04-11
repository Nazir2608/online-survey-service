package com.nazir.onlinesurveyservice.service.impl;

import com.nazir.onlinesurveyservice.domain.entity.*;
import com.nazir.onlinesurveyservice.domain.enums.QuestionType;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.SubmitResponseRequest;
import com.nazir.onlinesurveyservice.dto.response.*;
import com.nazir.onlinesurveyservice.exception.ForbiddenException;
import com.nazir.onlinesurveyservice.exception.ResourceNotFoundException;
import com.nazir.onlinesurveyservice.exception.SurveyStateException;
import com.nazir.onlinesurveyservice.repository.*;
import com.nazir.onlinesurveyservice.service.ResponseService;
import com.nazir.onlinesurveyservice.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseServiceImpl implements ResponseService {

    private final SurveyRepository   surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository   optionRepository;
    private final ResponseRepository responseRepository;
    private final AnswerRepository   answerRepository;

    @Override
    @Transactional
    public ResponseSummaryResponse submit(UUID surveyId,
                                           SubmitResponseRequest request,
                                           User currentUser,
                                           HttpServletRequest httpRequest) {
        Survey survey = findSurveyOrThrow(surveyId);

        // Guard: survey must be PUBLISHED
        if (survey.getStatus() != SurveyStatus.PUBLISHED) {
            throw new SurveyStateException("Survey is not open for responses");
        }

        // Guard: anonymous check
        if (!survey.isAnonymous() && currentUser == null) {
            throw new ForbiddenException("This survey requires authentication");
        }

        // Guard: duplicate submission
        if (currentUser != null
                && responseRepository.existsBySurveyIdAndRespondentId(surveyId, currentUser.getId())) {
            throw new SurveyStateException("You have already submitted a response to this survey");
        }

        // Guard: max responses
        if (survey.getMaxResponses() != null) {
            long count = responseRepository.countBySurveyId(surveyId);
            if (count >= survey.getMaxResponses()) {
                throw new SurveyStateException("This survey has reached its maximum number of responses");
            }
        }

        List<Question> questions = questionRepository.findBySurveyIdOrderByOrderIndexAsc(surveyId);

        // Validate required questions are answered
        Map<UUID, SubmitResponseRequest.AnswerRequest> answerMap = request.answers().stream()
                .collect(Collectors.toMap(SubmitResponseRequest.AnswerRequest::questionId, a -> a));

        for (Question q : questions) {
            if (q.isRequired() && !hasValue(answerMap.get(q.getId()))) {
                throw new IllegalArgumentException("Question '" + q.getText() + "' is required");
            }
        }

        // Build Response
        Response response = Response.builder()
                .survey(survey)
                .respondent(currentUser)
                .ipAddress(HashUtil.sha256(getClientIp(httpRequest)))
                .userAgent(truncate(httpRequest.getHeader("User-Agent"), 500))
                .submittedAt(Instant.now())
                .build();

        // Build Answers
        Map<UUID, Question> questionLookup = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        for (SubmitResponseRequest.AnswerRequest ar : request.answers()) {
            Question q = questionLookup.get(ar.questionId());
            if (q == null) continue;

            Answer answer = Answer.builder()
                    .response(response)
                    .question(q)
                    .build();

            switch (q.getType()) {
                case TEXT, PARAGRAPH, DATE, EMAIL, NUMBER -> answer.setTextValue(ar.textValue());
                case RATING                               -> answer.setRatingValue(ar.ratingValue());
                case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                    if (ar.selectedOptionIds() != null) {
                        Set<Option> selected = ar.selectedOptionIds().stream()
                                .map(oid -> optionRepository.findById(oid)
                                        .orElseThrow(() -> new ResourceNotFoundException("Option", oid)))
                                .collect(Collectors.toSet());
                        answer.setSelectedOptions(selected);
                    }
                }
            }
            response.getAnswers().add(answer);
        }

        responseRepository.save(response);
        log.info("Response submitted for survey [{}] by [{}]",
                surveyId, currentUser != null ? currentUser.getEmail() : "anonymous");

        return toSummary(response);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResponseSummaryResponse> listBySurvey(UUID surveyId,
                                                               User currentUser,
                                                               Pageable pageable) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);
        return PageResponse.of(responseRepository.findBySurveyId(surveyId, pageable), this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseSummaryResponse getById(UUID surveyId, UUID responseId, User currentUser) {
        Survey   survey   = findSurveyOrThrow(surveyId);
        Response response = responseRepository.findByIdAndSurveyId(responseId, surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Response", responseId));

        boolean isOwner  = survey.getCreator().getId().equals(currentUser.getId());
        boolean isAdmin  = currentUser.getRole() == Role.ADMIN;
        boolean isRespondent = response.getRespondent() != null
                && response.getRespondent().getId().equals(currentUser.getId());

        if (!isOwner && !isAdmin && !isRespondent) {
            throw new ForbiddenException("Access denied");
        }
        return toSummary(response);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UUID surveyId, User currentUser) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);

        long totalResponses = responseRepository.countBySurveyId(surveyId);
        List<Question> questions = questionRepository.findBySurveyIdOrderByOrderIndexAsc(surveyId);

        List<AnalyticsResponse.QuestionStats> stats = questions.stream()
                .map(q -> buildQuestionStats(q, totalResponses))
                .toList();

        // Daily trend — last 30 days
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<AnalyticsResponse.DailyCount> trend = responseRepository
                .countByDay(surveyId, since).stream()
                .map(row -> new AnalyticsResponse.DailyCount(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((Number) row[1]).longValue()))
                .toList();

        return new AnalyticsResponse(surveyId, survey.getTitle(), totalResponses, stats, trend);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID surveyId, User currentUser) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);

        List<Question> questions = questionRepository.findBySurveyIdOrderByOrderIndexAsc(surveyId);
        List<Response> responses = responseRepository.findBySurveyId(
                surveyId, Pageable.unpaged()).getContent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8);

        // Header row
        List<String> headers = new ArrayList<>();
        headers.add("Response ID");
        headers.add("Respondent");
        headers.add("Submitted At");
        questions.forEach(q -> headers.add(csvEscape(q.getText())));
        pw.println(String.join(",", headers));

        // Data rows
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC"));
        for (Response r : responses) {
            Map<UUID, Answer> ansMap = r.getAnswers().stream()
                    .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

            List<String> row = new ArrayList<>();
            row.add(r.getId().toString());
            row.add(r.getRespondent() != null ? r.getRespondent().getDisplayName() : "Anonymous");
            row.add(fmt.format(r.getSubmittedAt()));

            for (Question q : questions) {
                Answer ans = ansMap.get(q.getId());
                row.add(ans == null ? "" : csvEscape(formatAnswer(ans)));
            }
            pw.println(String.join(",", row));
        }

        pw.flush();
        return baos.toByteArray();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Survey findSurveyOrThrow(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
    }

    private void assertOwnerOrAdmin(Survey survey, User user) {
        if (user.getRole() != Role.ADMIN
                && !survey.getCreator().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private boolean hasValue(SubmitResponseRequest.AnswerRequest ar) {
        if (ar == null) return false;
        return (ar.textValue() != null && !ar.textValue().isBlank())
                || ar.ratingValue() != null
                || (ar.selectedOptionIds() != null && !ar.selectedOptionIds().isEmpty());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String formatAnswer(Answer ans) {
        if (ans.getTextValue() != null)   return ans.getTextValue();
        if (ans.getRatingValue() != null) return ans.getRatingValue().toString();
        if (!ans.getSelectedOptions().isEmpty()) {
            return ans.getSelectedOptions().stream()
                    .map(Option::getText)
                    .collect(Collectors.joining("; "));
        }
        return "";
    }

    private AnalyticsResponse.QuestionStats buildQuestionStats(Question q, long totalResponses) {
        QuestionType type = q.getType();

        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE) {
            List<Object[]> rows = answerRepository.countOptionSelections(q.getId());
            List<AnalyticsResponse.OptionCount> counts = rows.stream().map(row ->
                    new AnalyticsResponse.OptionCount(
                            (UUID)   row[0],
                            (String) row[1],
                            ((Number) row[2]).longValue(),
                            totalResponses > 0
                                    ? ((Number) row[2]).doubleValue() / totalResponses * 100.0
                                    : 0.0
                    )).toList();

            return new AnalyticsResponse.QuestionStats(
                    q.getId(), q.getText(), type.name(),
                    counts, null, null, null, counts.stream().mapToLong(AnalyticsResponse.OptionCount::count).sum());
        }

        if (type == QuestionType.RATING || type == QuestionType.NUMBER) {
            Object[] stats = answerRepository.ratingStats(q.getId());
            Double avg = stats[0] != null ? ((Number) stats[0]).doubleValue() : null;
            Integer min = stats[1] != null ? ((Number) stats[1]).intValue() : null;
            Integer max = stats[2] != null ? ((Number) stats[2]).intValue() : null;
            return new AnalyticsResponse.QuestionStats(
                    q.getId(), q.getText(), type.name(),
                    List.of(), avg, min, max, totalResponses);
        }

        // TEXT / PARAGRAPH / DATE / EMAIL
        return new AnalyticsResponse.QuestionStats(
                q.getId(), q.getText(), type.name(),
                List.of(), null, null, null, totalResponses);
    }

    private ResponseSummaryResponse toSummary(Response r) {
        ResponseSummaryResponse.RespondentSummary respondent = null;
        if (r.getRespondent() != null) {
            respondent = new ResponseSummaryResponse.RespondentSummary(
                    r.getRespondent().getId(), r.getRespondent().getDisplayName());
        }
        return new ResponseSummaryResponse(
                r.getId(), r.getSurvey().getId(), respondent, r.getSubmittedAt());
    }
}
