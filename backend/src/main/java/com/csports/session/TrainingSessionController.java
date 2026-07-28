package com.csports.session;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csports.session.dto.CreateTrainingSessionRequest;
import com.csports.common.pagination.PageResponse;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.session.dto.TrainingSessionDetailsResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.TrainingSessionService;
import com.csports.common.web.ApiPaths;

@RestController
@RequestMapping({ApiPaths.SESSIONS, ApiPaths.LEGACY_SESSIONS})
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PostMapping
    public ResponseEntity<String> createSession(@RequestBody CreateTrainingSessionRequest request) {

        trainingSessionService.createSession(request);

        return ResponseEntity.ok("Training session created successfully.");
    }

    @GetMapping
    public PageResponse<TrainingSessionResponse> getSessions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return trainingSessionService.getAllUpcomingSessions(page, size);
    }

    @GetMapping("/{sessionId}")
    public TrainingSessionDetailsResponse getSession(@PathVariable Long sessionId) {
        return trainingSessionService.getSession(sessionId);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> deleteSession(@PathVariable Long sessionId) {

        trainingSessionService.deleteSession(sessionId);

        return ResponseEntity.ok("Training session deleted successfully.");
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/{sessionId}/participants")
    public PageResponse<SessionParticipantResponse> getParticipants(@PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return trainingSessionService.getParticipants(sessionId, page, size);
    }
}
