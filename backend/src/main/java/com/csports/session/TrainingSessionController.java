package com.csports.session;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;
import com.csports.session.dto.CreateTrainingSessionRequest;
import com.csports.session.dto.CancelTrainingSessionRequest;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.session.dto.SessionSearchRequest;
import com.csports.session.dto.TrainingSessionDetailsResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.dto.UpdateTrainingSessionRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping({ApiPaths.SESSIONS, ApiPaths.LEGACY_SESSIONS})
@Validated
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PostMapping
    public ResponseEntity<TrainingSessionResponse> createSession(
            @Valid @RequestBody CreateTrainingSessionRequest request) {
        TrainingSessionResponse response = trainingSessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<TrainingSessionResponse> getSessions(
            @Valid @ModelAttribute SessionSearchRequest request) {
        return trainingSessionService.searchSessions(request);
    }

    @GetMapping("/{sessionId}")
    public TrainingSessionDetailsResponse getSession(@PathVariable @Positive Long sessionId) {
        return trainingSessionService.getSession(sessionId);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{sessionId}")
    public ResponseEntity<TrainingSessionResponse> updateSession(
            @PathVariable @Positive Long sessionId,
            @Valid @RequestBody UpdateTrainingSessionRequest request) {
        TrainingSessionResponse updatedSession =
                trainingSessionService.updateSession(sessionId, request);
        return ResponseEntity.ok(updatedSession);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{sessionId}/cancel")
    public ResponseEntity<String> cancelSession(
            @PathVariable @Positive Long sessionId,
            @Valid @RequestBody CancelTrainingSessionRequest request) {
        trainingSessionService.cancelSession(sessionId, request);
        return ResponseEntity.ok("Training session cancelled successfully.");
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{sessionId}/restore")
    public ResponseEntity<String> restoreSession(@PathVariable @Positive Long sessionId) {
        trainingSessionService.restoreSession(sessionId);
        return ResponseEntity.ok("Training session restored successfully.");
    }

    /**
     * Backward-compatible route for current clients. It now performs a safe
     * cancellation so bookings and notifications are preserved.
     */
    @PreAuthorize("hasRole('TRAINER')")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> deleteSession(
            @PathVariable @Positive Long sessionId,
            @Valid @RequestBody CancelTrainingSessionRequest request) {
        trainingSessionService.deleteSession(sessionId, request);
        return ResponseEntity.ok("Training session cancelled successfully.");
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/{sessionId}/participants")
    public PageResponse<SessionParticipantResponse> getParticipants(
            @PathVariable @Positive Long sessionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return trainingSessionService.getParticipants(sessionId, page, size);
    }
}
