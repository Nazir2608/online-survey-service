package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.dto.request.CreateQuestionRequest;
import com.nazir.onlinesurveyservice.dto.request.QuestionReorderRequest;
import com.nazir.onlinesurveyservice.dto.response.QuestionResponse;

import java.util.List;
import java.util.UUID;

public interface QuestionService {

    QuestionResponse        create(UUID surveyId, User currentUser, CreateQuestionRequest request);

    List<QuestionResponse>  listBySurvey(UUID surveyId);

    QuestionResponse        getById(UUID surveyId, UUID questionId);

    QuestionResponse        update(UUID surveyId, UUID questionId, User currentUser, CreateQuestionRequest request);

    void                    delete(UUID surveyId, UUID questionId, User currentUser);

    List<QuestionResponse>  reorder(UUID surveyId, User currentUser, QuestionReorderRequest request);
}
