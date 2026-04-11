package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.dto.request.SubmitResponseRequest;
import com.nazir.onlinesurveyservice.dto.response.AnalyticsResponse;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.ResponseSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResponseService {

    ResponseSummaryResponse       submit(UUID surveyId,
                                         SubmitResponseRequest request,
                                         User currentUser,           // nullable for anonymous
                                         HttpServletRequest httpRequest);

    PageResponse<ResponseSummaryResponse> listBySurvey(UUID surveyId,
                                                        User currentUser,
                                                        Pageable pageable);

    ResponseSummaryResponse       getById(UUID surveyId, UUID responseId, User currentUser);

    AnalyticsResponse             getAnalytics(UUID surveyId, User currentUser);

    byte[]                        exportCsv(UUID surveyId, User currentUser);
}
