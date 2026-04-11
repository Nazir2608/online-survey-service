package com.nazir.onlinesurveyservice.controller;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.dto.request.CreateQuestionRequest;
import com.nazir.onlinesurveyservice.dto.request.QuestionReorderRequest;
import com.nazir.onlinesurveyservice.dto.response.QuestionResponse;
import com.nazir.onlinesurveyservice.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Questions", description = "Manage questions within a survey")
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "Add a question to a survey")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuestionResponse> create(
            @PathVariable UUID surveyId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.create(surveyId, user, request));
    }

    @Operation(summary = "List all questions for a survey")
    @GetMapping
    public ResponseEntity<List<QuestionResponse>> list(@PathVariable UUID surveyId) {
        return ResponseEntity.ok(questionService.listBySurvey(surveyId));
    }

    @Operation(summary = "Get a single question")
    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionResponse> getById(
            @PathVariable UUID surveyId,
            @PathVariable UUID questionId) {
        return ResponseEntity.ok(questionService.getById(surveyId, questionId));
    }

    @Operation(summary = "Update a question")
    @PutMapping("/{questionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuestionResponse> update(
            @PathVariable UUID surveyId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.update(surveyId, questionId, user, request));
    }

    @Operation(summary = "Delete a question")
    @DeleteMapping("/{questionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID surveyId,
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User user) {
        questionService.delete(surveyId, questionId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder questions — pass ordered list of question IDs")
    @PutMapping("/reorder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<QuestionResponse>> reorder(
            @PathVariable UUID surveyId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody QuestionReorderRequest request) {
        return ResponseEntity.ok(questionService.reorder(surveyId, user, request));
    }
}
