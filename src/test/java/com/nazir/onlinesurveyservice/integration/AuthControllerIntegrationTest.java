package com.nazir.onlinesurveyservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazir.onlinesurveyservice.dto.request.LoginRequest;
import com.nazir.onlinesurveyservice.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth API integration tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    @DisplayName("POST /api/v1/auth/register → 201 with tokens")
    void register_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "integration@example.com", "password123", "Integration User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"))
                .andExpect(jsonPath("$.user.role").value("RESPONDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register → 409 when email duplicate")
    void register_duplicate_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com", "password123", "Dup User");

        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration — same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register → 400 when payload invalid")
    void register_invalidPayload_returns400() throws Exception {
        RegisterRequest bad = new RegisterRequest("not-an-email", "short", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login → 200 with tokens after registration")
    void login_success() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest(
                "login@example.com", "mypassword1", "Login User");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest login = new LoginRequest("login@example.com", "mypassword1");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
