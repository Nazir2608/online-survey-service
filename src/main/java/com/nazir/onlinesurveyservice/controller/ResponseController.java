package com.nazir.onlinesurveyservice.controller;

import com.nazir.onlinesurveyservice.domain.entity.User;
import com.nazir.onlinesurveyservice.dto.request.SubmitResponseRequest;
import com.nazir.onlinesurveyservice.dto.response.AnalyticsResponse;
import com.nazir.onlinesurveyservice.dto.response.PageResponse;
import com.nazir.onlinesurveyservice.dto.response.ResponseSummaryResponse;
import com.nazir.onlinesurveyservice.service.ResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Responses", description = "Submit responses and view results")
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseService responseService;

    @Operation(summary = "Submit a response to a survey")
    @PostMapping
    public ResponseEntity<ResponseSummaryResponse> submit(@PathVariable UUID surveyId, @Valid @RequestBody SubmitResponseRequest request, @AuthenticationPrincipal User user, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseService.submit(surveyId, request, user, httpRequest));
    }

    @Operation(summary = "[Creator/Admin] List all responses for a survey")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ResponseSummaryResponse>> list(@PathVariable UUID surveyId, @AuthenticationPrincipal User user, @RequestParam(defaultValue = "0")  int page, @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return ResponseEntity.ok(responseService.listBySurvey(surveyId, user, pageable));
    }

    @Operation(summary = "Get a single response by ID")
    @GetMapping("/{responseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseSummaryResponse> getById(@PathVariable UUID surveyId, @PathVariable UUID responseId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(responseService.getById(surveyId, responseId, user));
    }

    @Operation(summary = "[Creator/Admin] Get analytics / aggregate stats for a survey")
    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyticsResponse> analytics(@PathVariable UUID surveyId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(responseService.getAnalytics(surveyId, user));
    }

    @Operation(summary = "[Creator/Admin] Export all responses as CSV")
    @GetMapping("/export/csv")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID surveyId, @AuthenticationPrincipal User user) {
        byte[] csv = responseService.exportCsv(surveyId, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"survey-" + surveyId + "-responses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
