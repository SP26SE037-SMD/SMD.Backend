package com.example.smd.controller;

import com.example.smd.dto.request.subject.SubjectPublishRequest;
import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.services.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Subject", description = "Endpoints for managing academic subjects and their metadata")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    SubjectService subjectService;

    @PostMapping
    @Operation(summary = "Create a new subject", description = "Registers a new subject in the system with unique subject code")
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    public ResponseObject<SubjectResponse> create(@RequestBody @Valid SubjectRequest request) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.create(request))
                .message("Subject created successfully")
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Advanced search subjects with pagination",
            description = "Search by 'code' or 'name' directly at the database level. Filters by status is also supported."
    )
    public ResponseObject<PagedResponse<SubjectResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "code") String searchBy,
            @Parameter(description = "Filter by status: null (Draft), true (Published), false (Hidden)")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "subjectCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<SubjectResponse>>builder()
                .data(PagedResponse.of(subjectService.getAll(search, searchBy,status, pageable)))
                .message("Subjects retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get subject detail by ID",
            description = "Retrieves full subject details including Department and CLOs using JOIN FETCH for optimal performance."
    )
    public ResponseObject<SubjectResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.getDetail(id))
                .message("Subject detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject information")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    public ResponseObject<SubjectResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectRequest request) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.update(id, request))
                .message("Subject updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT_DELETE')")
    @Operation(summary = "Soft delete subject", description = "Sets subject status to inactive instead of hard deleting from database")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        subjectService.delete(id);
        return ResponseObject.<Void>builder()
                .message("Subject deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('SUBJECT_PUBLISH')")
    @Operation(
            summary = "Publish a subject syllabus",
            description = "Change subject status from Draft to Published. Requires 'SUBJECT_PUBLISH' authority and a valid decision number."
    )
    public ResponseObject<SubjectResponse> publish(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectPublishRequest request) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.publishSubject(id, request.getDecisionNo()))
                .message("Subject published successfully with decision: " + request.getDecisionNo())
                .build();
    }

    @PatchMapping("/{id}/internal-review")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    @Operation(
            summary = "Move subject to internal review",
            description = "Transitions the subject status from DRAFT to INTERNAL_REVIEW. " +
                    "This allows faculty members and experts to access the subject for internal auditing and feedback."
    )
    public ResponseObject<SubjectResponse> publishInternal(@PathVariable UUID id) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.publishSubjectInternal(id))
                .message("Subject has been successfully moved to internal review status.")
                .build();
    }
}
