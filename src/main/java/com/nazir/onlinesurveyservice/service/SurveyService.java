package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.CreateSurveyRequest;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.SurveyDetailResponse;
import com.nazir.onlinesurveyservice.dto.response.SurveyResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SurveyService {

    SurveyResponse        create(User creator, CreateSurveyRequest request);

    SurveyDetailResponse  getById(UUID id, User currentUser);

    SurveyDetailResponse  getBySlug(String slug, User currentUser);

    PageResponse<SurveyResponse> listPublished(String keyword, Pageable pageable);

    PageResponse<SurveyResponse> listMySurveys(User creator, SurveyStatus status, Pageable pageable);

    SurveyResponse        update(UUID id, User currentUser, CreateSurveyRequest request);

    SurveyResponse        changeStatus(UUID id, User currentUser, SurveyStatus newStatus);

    SurveyResponse        duplicate(UUID id, User currentUser);

    void                  delete(UUID id, User currentUser);
}
