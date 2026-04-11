package com.nazir.onlinesurveyservice.controller;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.domain.enums.SurveyStatus;
import com.nazir.onlinesurveyservice.dto.request.CreateSurveyRequest;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.SurveyDetailResponse;
import com.nazir.onlinesurveyservice.dto.response.SurveyResponse;
import com.nazir.onlinesurveyservice.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Surveys", description = "Create, manage and discover surveys")
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "Create a new survey (DRAFT)")
    @PostMapping
    @PreAuthorize("hasAnyRole('CREATOR','ADMIN')")
    public ResponseEntity<SurveyResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyService.create(user, request));
    }

    @Operation(summary = "List all published surveys (public)")
    @GetMapping
    public ResponseEntity<PageResponse<SurveyResponse>> listPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(surveyService.listPublished(keyword, pageable));
    }

    @Operation(summary = "List my own surveys")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CREATOR','ADMIN')")
    public ResponseEntity<PageResponse<SurveyResponse>> listMySurveys(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) SurveyStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(surveyService.listMySurveys(user, status, pageable));
    }

    @Operation(summary = "Get survey by ID")
    @GetMapping("/{id}")
    public ResponseEntity<SurveyDetailResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(surveyService.getById(id, user));
    }

    @Operation(summary = "Get survey by slug (public URL)")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<SurveyDetailResponse> getBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(surveyService.getBySlug(slug, user));
    }

    @Operation(summary = "Update survey (DRAFT only)")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurveyResponse> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSurveyRequest request) {
        return ResponseEntity.ok(surveyService.update(id, user, request));
    }

    @Operation(summary = "Change survey status (DRAFT→PUBLISHED→CLOSED→ARCHIVED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurveyResponse> changeStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @RequestParam SurveyStatus status) {
        return ResponseEntity.ok(surveyService.changeStatus(id, user, status));
    }

    @Operation(summary = "Duplicate an existing survey")
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyRole('CREATOR','ADMIN')")
    public ResponseEntity<SurveyResponse> duplicate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyService.duplicate(id, user));
    }

    @Operation(summary = "Delete a survey")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        surveyService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
