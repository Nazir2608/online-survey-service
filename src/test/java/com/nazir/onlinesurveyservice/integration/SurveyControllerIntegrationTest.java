package com.nazir.onlinesurveyservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazir.onlinesurveyservice.dto.request.CreateSurveyRequest;
import com.nazir.onlinesurveyservice.dto.request.LoginRequest;
import com.nazir.onlinesurveyservice.dto.request.RegisterRequest;
import com.nazir.onlinesurveyservice.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Survey API integration tests")
class SurveyControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    private String creatorToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register a CREATOR user and obtain JWT
        RegisterRequest reg = new RegisterRequest(
                "survey_creator@example.com", "password123", "Survey Creator");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reg)))
                .andReturn();

        // If already exists, just login
        if (regResult.getResponse().getStatus() == 409) {
            LoginRequest login = new LoginRequest("survey_creator@example.com", "password123");
            regResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(login)))
                    .andReturn();
        }

        AuthResponse auth = mapper.readValue(
                regResult.getResponse().getContentAsString(), AuthResponse.class);
        creatorToken = auth.accessToken();
    }

    @Test
    @DisplayName("POST /api/v1/surveys → 403 for unauthenticated user")
    void create_survey_unauthenticated_returns403() throws Exception {
        CreateSurveyRequest req = new CreateSurveyRequest(
                "My Survey", "Desc", false, null, null, null);

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/surveys → 200 with paginated list (public)")
    void list_published_surveys_public() throws Exception {
        mockMvc.perform(get("/api/v1/surveys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("Full survey lifecycle: create → publish → view")
    void full_survey_lifecycle() throws Exception {
        // 1. Create survey
        CreateSurveyRequest req = new CreateSurveyRequest(
                "Lifecycle Test Survey", "A test", false, null, null, null);

        MvcResult createResult = mockMvc.perform(post("/api/v1/surveys")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String surveyId = mapper.readTree(
                createResult.getResponse().getContentAsString())
                .get("id").asText();

        // 2. Publish survey
        mockMvc.perform(patch("/api/v1/surveys/" + surveyId + "/status")
                        .header("Authorization", "Bearer " + creatorToken)
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // 3. View survey (public — no token)
        mockMvc.perform(get("/api/v1/surveys/" + surveyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lifecycle Test Survey"));
    }
}
