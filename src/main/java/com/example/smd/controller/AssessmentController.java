package com.example.smd.controller;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Assessment", description = "Assessment Management APIs")
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    @Operation(summary = "Get all assessments with pagination and filters")
    public ResponseObject<PagedResponse<AssessmentResponse>> getAllAssessments(
            @RequestParam(required = false) UUID syllabusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "part,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<AssessmentResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(assessmentService.getAllAssessments(syllabusId, status, search, page, size, sort)))
                .message("Get all assessments successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by ID")
    public ResponseObject<AssessmentResponse> getAssessmentById(@PathVariable String id) {
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.getAssessmentById(id))
                .message("Get assessment successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all assessments by syllabus")
    public ResponseObject<List<AssessmentResponse>> getAssessmentsBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<AssessmentResponse>>builder()
                .status(1000)
                .data(assessmentService.getAssessmentsBySyllabus(syllabusId))
                .message("Get assessments by syllabus successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new assessment")
    public ResponseObject<AssessmentResponse> createAssessment(@Valid @RequestBody AssessmentRequest request) {
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.createAssessment(request))
                .message("Create assessment successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update assessment by ID")
    public ResponseObject<AssessmentResponse> updateAssessment(
            @PathVariable String id,
            @Valid @RequestBody AssessmentRequest request
    ) {
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.updateAssessment(id, request))
                .message("Update assessment successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update assessment status")
    public ResponseObject<AssessmentResponse> updateAssessmentStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.updateAssessmentStatus(id, status))
                .message("Update assessment status successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Soft delete assessment")
    public ResponseObject<Boolean> deleteAssessment(@PathVariable String id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(assessmentService.deleteAssessment(id))
                .message("Delete assessment successfully")
                .build();
    }
}
