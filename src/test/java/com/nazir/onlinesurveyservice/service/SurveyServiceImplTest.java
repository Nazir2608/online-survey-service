package com.nazir.onlinesurveyservice.service;

import com.nazir.onlinesurveyservice.domain.entity.Survey;
import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.Role;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.CreateSurveyRequest;
import com.nazir.onlinesurveyservice.dto.response.SurveyResponse;
import com.nazir.onlinesurveyservice.exception.ForbiddenException;
import com.nazir.onlinesurveyservice.exception.SurveyStateException;
import com.nazir.onlinesurveyservice.repository.SurveyRepository;
import com.nazir.onlinesurveyservice.service.impl.SurveyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyService unit tests")
class SurveyServiceImplTest {

    @Mock SurveyRepository surveyRepository;
    @InjectMocks SurveyServiceImpl surveyService;

    private User creator;
    private User otherUser;

    @BeforeEach
    void setUp() {
        creator = User.builder()
                .email("creator@example.com")
                .displayName("Creator")
                .role(Role.CREATOR)
                .build();
        // Inject UUID via reflection since @GeneratedValue won't fire in unit test
        setId(creator, UUID.randomUUID());

        otherUser = User.builder()
                .email("other@example.com")
                .displayName("Other")
                .role(Role.RESPONDENT)
                .build();
        setId(otherUser, UUID.randomUUID());
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates survey in DRAFT status")
        void create_returnsDraftSurvey() {
            CreateSurveyRequest req = new CreateSurveyRequest(
                    "My Survey", "Description", false, null, null, null);

            when(surveyRepository.save(any(Survey.class)))
                    .thenAnswer(inv -> {
                        Survey s = inv.getArgument(0);
                        setId(s, UUID.randomUUID());
                        return s;
                    });

            SurveyResponse response = surveyService.create(creator, req);

            assertThat(response.title()).isEqualTo("My Survey");
            assertThat(response.status()).isEqualTo(SurveyStatus.DRAFT);
            verify(surveyRepository).save(any(Survey.class));
        }
    }

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatus {

        @Test
        @DisplayName("DRAFT → PUBLISHED succeeds for owner")
        void draft_to_published_succeeds() {
            Survey survey = buildDraftSurvey();
            when(surveyRepository.findById(survey.getId())).thenReturn(Optional.of(survey));
            when(surveyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SurveyResponse result = surveyService.changeStatus(
                    survey.getId(), creator, SurveyStatus.PUBLISHED);

            assertThat(result.status()).isEqualTo(SurveyStatus.PUBLISHED);
        }

        @Test
        @DisplayName("DRAFT → ARCHIVED throws SurveyStateException")
        void invalid_transition_throws() {
            Survey survey = buildDraftSurvey();
            when(surveyRepository.findById(survey.getId())).thenReturn(Optional.of(survey));

            assertThatThrownBy(() ->
                    surveyService.changeStatus(survey.getId(), creator, SurveyStatus.ARCHIVED))
                    .isInstanceOf(SurveyStateException.class);
        }

        @Test
        @DisplayName("non-owner gets ForbiddenException")
        void nonOwner_changeStatus_forbidden() {
            Survey survey = buildDraftSurvey();
            when(surveyRepository.findById(survey.getId())).thenReturn(Optional.of(survey));

            assertThatThrownBy(() ->
                    surveyService.changeStatus(survey.getId(), otherUser, SurveyStatus.PUBLISHED))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Survey buildDraftSurvey() {
        Survey s = Survey.builder()
                .creator(creator)
                .title("Test Survey")
                .slug("test-survey-abc12345")
                .status(SurveyStatus.DRAFT)
                .build();
        setId(s, UUID.randomUUID());
        return s;
    }

    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
