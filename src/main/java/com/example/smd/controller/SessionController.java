package com.example.smd.controller;

import com.example.smd.dto.request.SessionRequest;
import com.example.smd.dto.request.SessionItemRequest;
import com.example.smd.dto.request.SessionNumberListRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.services.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Session", description = "Session Management APIs")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "Get all sessions with pagination and filters")
    public ResponseObject<PagedResponse<SessionResponse>> getAllSessions(
            @RequestParam(required = false) UUID syllabusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sessionNumber,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<SessionResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(sessionService.getAllSessions(
                        syllabusId, status, search, page, size, sort
                )))
                .message("Get all sessions successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    public ResponseObject<SessionResponse> getSessionById(@PathVariable UUID id) {
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.getSessionById(id))
                .message("Get session successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all sessions by syllabus")
    public ResponseObject<List<SessionResponse>> getSessionsBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<SessionResponse>>builder()
                .status(1000)
                .data(sessionService.getSessionsBySyllabus(syllabusId))
                .message("Get sessions by syllabus successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new session")
    public ResponseObject<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.createSession(request))
                .message("Create session successfully")
                .build();
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create session list for a syllabus")
    public ResponseObject<List<SessionResponse>> createSessionList(
            @RequestParam UUID syllabusId,
            @RequestBody List<@Valid SessionItemRequest> requests
    ) {
        return ResponseObject.<List<SessionResponse>>builder()
                .status(1000)
                .data(sessionService.createSessionsBySyllabus(syllabusId, requests))
                .message("Create session list successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update session by ID")
    public ResponseObject<SessionResponse> updateSession(
            @PathVariable UUID id,
            @Valid @RequestBody SessionRequest request
    ) {
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.updateSession(id, request))
                .message("Update session successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update session status")
    public ResponseObject<SessionResponse> updateSessionStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.updateSessionStatus(id, status))
                .message("Update session status successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete session by status rule")
    public ResponseObject<Boolean> deleteSession(@PathVariable UUID id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(sessionService.deleteSession(id))
                .message("Delete session successfully")
                .build();
    }

        @DeleteMapping("/batch")
        @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
        @Operation(summary = "Delete session list by syllabus and session numbers")
        public ResponseObject<Boolean> deleteSessionList(
                        @RequestParam UUID syllabusId,
                        @Valid @RequestBody SessionNumberListRequest request
        ) {
                return ResponseObject.<Boolean>builder()
                                .status(1000)
                                .data(sessionService.deleteSessionListBySyllabusAndSessionNumbers(syllabusId, request))
                                .message("Delete session list successfully")
                                .build();
        }
}
