package com.nazir.onlinesurveyservice.service.impl;

import com.nazir.onlinesurveyservice.domain.entity.Question;
import com.nazir.onlinesurveyservice.domain.entity.Survey;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.CreateSurveyRequest;
import com.nazir.onlinesurveyservice.dto.response.*;
import com.nazir.onlinesurveyservice.exception.ForbiddenException;
import com.nazir.onlinesurveyservice.exception.ResourceNotFoundException;
import com.nazir.onlinesurveyservice.exception.SurveyStateException;
import com.nazir.onlinesurveyservice.repository.SurveyRepository;
import com.nazir.onlinesurveyservice.service.SurveyService;
import com.nazir.onlinesurveyservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyServiceImpl implements SurveyService {

    private final SurveyRepository surveyRepository;

    // ─── Valid status transitions ──────────────────────────────────────────────
    private static boolean isValidTransition(SurveyStatus from, SurveyStatus to) {
        return switch (from) {
            case DRAFT     -> to == SurveyStatus.PUBLISHED;
            case PUBLISHED -> to == SurveyStatus.CLOSED;
            case CLOSED    -> to == SurveyStatus.ARCHIVED;
            case ARCHIVED  -> false;
        };
    }

    @Override
    @Transactional
    public SurveyResponse create(User creator, CreateSurveyRequest request) {
        Survey survey = Survey.builder()
                .creator(creator)
                .title(request.title())
                .description(request.description())
                .slug(SlugUtil.generate(request.title()))
                .anonymous(request.anonymous())
                .maxResponses(request.maxResponses())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        return toSummary(surveyRepository.save(survey));
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyDetailResponse getById(UUID id, User currentUser) {
        Survey survey = findOrThrow(id);
        assertCanView(survey, currentUser);
        return toDetail(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public SurveyDetailResponse getBySlug(String slug, User currentUser) {
        Survey survey = surveyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with slug: " + slug));
        assertCanView(survey, currentUser);
        return toDetail(survey);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SurveyResponse> listPublished(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return PageResponse.of(
                    surveyRepository.searchPublished(keyword, pageable), this::toSummary);
        }
        return PageResponse.of(
                surveyRepository.findByStatus(SurveyStatus.PUBLISHED, pageable), this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SurveyResponse> listMySurveys(User creator, SurveyStatus status, Pageable pageable) {
        var page = (status != null)
                ? surveyRepository.findByCreatorAndStatus(creator, status, pageable)
                : surveyRepository.findByCreator(creator, pageable);
        return PageResponse.of(page, this::toSummary);
    }

    @Override
    @Transactional
    public SurveyResponse update(UUID id, User currentUser, CreateSurveyRequest request) {
        Survey survey = findOrThrow(id);
        assertOwnerOrAdmin(survey, currentUser);

        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new SurveyStateException("Only DRAFT surveys can be edited");
        }

        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setAnonymous(request.anonymous());
        survey.setMaxResponses(request.maxResponses());
        survey.setStartDate(request.startDate());
        survey.setEndDate(request.endDate());

        return toSummary(surveyRepository.save(survey));
    }

    @Override
    @Transactional
    public SurveyResponse changeStatus(UUID id, User currentUser, SurveyStatus newStatus) {
        Survey survey = findOrThrow(id);
        assertOwnerOrAdmin(survey, currentUser);

        if (!isValidTransition(survey.getStatus(), newStatus)) {
            throw new SurveyStateException(
                    "Cannot transition from " + survey.getStatus() + " to " + newStatus);
        }

        survey.setStatus(newStatus);
        log.info("Survey [{}] status changed from {} to {} by user [{}]",
                id, survey.getStatus(), newStatus, currentUser.getEmail());
        return toSummary(surveyRepository.save(survey));
    }

    @Override
    @Transactional
    public SurveyResponse duplicate(UUID id, User currentUser) {
        Survey original = findOrThrow(id);
        assertOwnerOrAdmin(original, currentUser);

        Survey copy = Survey.builder()
                .creator(currentUser)
                .title("Copy of " + original.getTitle())
                .description(original.getDescription())
                .slug(SlugUtil.generate("Copy of " + original.getTitle()))
                .anonymous(original.isAnonymous())
                .maxResponses(original.getMaxResponses())
                .build();

        // Deep-copy questions + options
        original.getQuestions().forEach(q -> {
            Question newQ = Question.builder()
                    .survey(copy)
                    .text(q.getText())
                    .helpText(q.getHelpText())
                    .type(q.getType())
                    .orderIndex(q.getOrderIndex())
                    .required(q.isRequired())
                    .minValue(q.getMinValue())
                    .maxValue(q.getMaxValue())
                    .build();
            q.getOptions().forEach(o ->
                    newQ.getOptions().add(
                            com.nazir.onlinesurveyservice.domain.entity.Option.builder()
                                    .question(newQ)
                                    .text(o.getText())
                                    .orderIndex(o.getOrderIndex())
                                    .build()));
            copy.getQuestions().add(newQ);
        });

        return toSummary(surveyRepository.save(copy));
    }

    @Override
    @Transactional
    public void delete(UUID id, User currentUser) {
        Survey survey = findOrThrow(id);
        assertOwnerOrAdmin(survey, currentUser);
        surveyRepository.delete(survey);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Survey findOrThrow(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
    }

    private void assertOwnerOrAdmin(Survey survey, User user) {
        if (user.getRole() != Role.ADMIN
                && !survey.getCreator().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this survey");
        }
    }

    private void assertCanView(Survey survey, User currentUser) {
        if (survey.getStatus() == SurveyStatus.PUBLISHED) return;
        if (currentUser == null) throw new ForbiddenException("Survey is not publicly accessible");
        if (currentUser.getRole() == Role.ADMIN) return;
        if (survey.getCreator().getId().equals(currentUser.getId())) return;
        throw new ForbiddenException("Survey is not publicly accessible");
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private SurveyResponse toSummary(Survey s) {
        return new SurveyResponse(
                s.getId(), s.getTitle(), s.getDescription(), s.getSlug(),
                s.getStatus(), s.isAnonymous(), s.getMaxResponses(),
                s.getStartDate(), s.getEndDate(),
                new SurveyResponse.CreatorSummary(
                        s.getCreator().getId(), s.getCreator().getDisplayName()),
                s.getQuestions().size(),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    private SurveyDetailResponse toDetail(Survey s) {
        List<QuestionResponse> questions = s.getQuestions().stream()
                .map(this::toQuestionResponse)
                .toList();
        return new SurveyDetailResponse(
                s.getId(), s.getTitle(), s.getDescription(), s.getSlug(),
                s.getStatus(), s.isAnonymous(), s.getMaxResponses(),
                s.getStartDate(), s.getEndDate(),
                new SurveyResponse.CreatorSummary(
                        s.getCreator().getId(), s.getCreator().getDisplayName()),
                questions, s.getCreatedAt(), s.getUpdatedAt());
    }

    private QuestionResponse toQuestionResponse(Question q) {
        List<OptionResponse> options = q.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getText(), o.getOrderIndex()))
                .toList();
        return new QuestionResponse(
                q.getId(), q.getText(), q.getHelpText(), q.getType(),
                q.getOrderIndex(), q.isRequired(), q.getMinValue(), q.getMaxValue(),
                options, q.getCreatedAt(), q.getUpdatedAt());
    }
}
