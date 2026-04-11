package com.nazir.onlinesurveyservice.service.impl;

import com.nazir.onlinesurveyservice.domain.entity.Option;
import com.nazir.onlinesurveyservice.domain.entity.Question;
import com.nazir.onlinesurveyservice.domain.entity.Survey;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.QuestionType;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.CreateOptionRequest;
import com.nazir.onlinesurveyservice.dto.request.CreateQuestionRequest;
import com.nazir.onlinesurveyservice.dto.request.QuestionReorderRequest;
import com.nazir.onlinesurveyservice.dto.response.OptionResponse;
import com.nazir.onlinesurveyservice.dto.response.QuestionResponse;
import com.nazir.onlinesurveyservice.exception.ForbiddenException;
import com.nazir.onlinesurveyservice.exception.ResourceNotFoundException;
import com.nazir.onlinesurveyservice.exception.SurveyStateException;
import com.nazir.onlinesurveyservice.repository.QuestionRepository;
import com.nazir.onlinesurveyservice.repository.SurveyRepository;
import com.nazir.onlinesurveyservice.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final SurveyRepository   surveyRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public QuestionResponse create(UUID surveyId, User currentUser, CreateQuestionRequest request) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);
        assertDraft(survey);

        int nextIndex = questionRepository.countBySurveyId(surveyId);

        Question question = Question.builder()
                .survey(survey)
                .text(request.text())
                .helpText(request.helpText())
                .type(request.type())
                .orderIndex(nextIndex)
                .required(request.required())
                .minValue(request.minValue())
                .maxValue(request.maxValue())
                .build();

        if (isChoiceType(request.type()) && request.options() != null) {
            addOptions(question, request.options());
        }

        return toResponse(questionRepository.save(question));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> listBySurvey(UUID surveyId) {
        findSurveyOrThrow(surveyId); // validate survey exists
        return questionRepository.findBySurveyIdOrderByOrderIndexAsc(surveyId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getById(UUID surveyId, UUID questionId) {
        return toResponse(findQuestionOrThrow(surveyId, questionId));
    }

    @Override
    @Transactional
    public QuestionResponse update(UUID surveyId, UUID questionId,
                                    User currentUser, CreateQuestionRequest request) {
        Survey   survey   = findSurveyOrThrow(surveyId);
        Question question = findQuestionOrThrow(surveyId, questionId);
        assertOwnerOrAdmin(survey, currentUser);
        assertDraft(survey);

        question.setText(request.text());
        question.setHelpText(request.helpText());
        question.setType(request.type());
        question.setRequired(request.required());
        question.setMinValue(request.minValue());
        question.setMaxValue(request.maxValue());

        // Rebuild options
        question.getOptions().clear();
        if (isChoiceType(request.type()) && request.options() != null) {
            addOptions(question, request.options());
        }

        return toResponse(questionRepository.save(question));
    }

    @Override
    @Transactional
    public void delete(UUID surveyId, UUID questionId, User currentUser) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);
        assertDraft(survey);

        Question question = findQuestionOrThrow(surveyId, questionId);
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public List<QuestionResponse> reorder(UUID surveyId, User currentUser,
                                           QuestionReorderRequest request) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertOwnerOrAdmin(survey, currentUser);
        assertDraft(survey);

        List<UUID> ids = request.questionIds();
        for (int i = 0; i < ids.size(); i++) {
            questionRepository.updateOrderIndex(ids.get(i), i);
        }

        return questionRepository.findBySurveyIdOrderByOrderIndexAsc(surveyId)
                .stream().map(this::toResponse).toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Survey findSurveyOrThrow(UUID id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey", id));
    }

    private Question findQuestionOrThrow(UUID surveyId, UUID questionId) {
        return questionRepository.findByIdAndSurveyId(questionId, surveyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question " + questionId + " not found in survey " + surveyId));
    }

    private void assertOwnerOrAdmin(Survey survey, User user) {
        if (user.getRole() != Role.ADMIN
                && !survey.getCreator().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this survey");
        }
    }

    private void assertDraft(Survey survey) {
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new SurveyStateException("Questions can only be modified when survey is in DRAFT status");
        }
    }

    private boolean isChoiceType(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE;
    }

    private void addOptions(Question question, List<CreateOptionRequest> optionRequests) {
        for (int i = 0; i < optionRequests.size(); i++) {
            Option opt = Option.builder()
                    .question(question)
                    .text(optionRequests.get(i).text())
                    .orderIndex(i)
                    .build();
            question.getOptions().add(opt);
        }
    }

    private QuestionResponse toResponse(Question q) {
        List<OptionResponse> options = q.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getText(), o.getOrderIndex()))
                .toList();
        return new QuestionResponse(
                q.getId(), q.getText(), q.getHelpText(), q.getType(),
                q.getOrderIndex(), q.isRequired(), q.getMinValue(), q.getMaxValue(),
                options, q.getCreatedAt(), q.getUpdatedAt());
    }
}
