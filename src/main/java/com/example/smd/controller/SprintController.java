package com.example.smd.controller;

import com.example.smd.dto.request.sprint.SprintRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.sprint.SprintResponse;
import com.example.smd.services.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Sprint", description = "Endpoints for managing agile sprints")
@SecurityRequirement(name = "bearerAuth")
public class SprintController {

    SprintService sprintService;

    @PostMapping
    @Operation(summary = "Create a new sprint")
    public ResponseObject<SprintResponse> create(@RequestBody @Valid SprintRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.create(request, userId))
                .message("Sprint created successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Advanced search sprints with pagination")
    public ResponseObject<PagedResponse<SprintResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID curriculumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<SprintResponse>>builder()
                .data(PagedResponse.of(sprintService.getAll(search, status, curriculumId, pageable, userId)))
                .message("Sprints retrieved successfully")
                .build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get sprints by account ID with pagination")
    public ResponseObject<PagedResponse<SprintResponse>> getSprintsByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<SprintResponse>>builder()
                .data(PagedResponse.of(sprintService.getSprintsByAccountId(accountId, pageable)))
                .message("Account sprints retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sprint detail by ID")
    public ResponseObject<SprintResponse> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.getDetail(id, userId))
                .message("Sprint detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update sprint information")
    public ResponseObject<SprintResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SprintRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.update(id, request, userId))
                .message("Sprint updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete sprint")
    public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        sprintService.delete(id, userId);
        return ResponseObject.<Void>builder()
                .message("Sprint deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update sprint status", description = "Allowed values: Planning, Active, Completed")
    public ResponseObject<SprintResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.updateStatus(id, status))
                .message("Sprint status updated successfully")
                .build();
    }
}
