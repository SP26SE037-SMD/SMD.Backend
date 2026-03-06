package com.example.smd.controller;

import com.example.smd.dto.request.PrerequisiteRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.PrerequisiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prerequisites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Prerequisite", description = "Endpoints for managing subject prerequisites")
public class PrerequisiteController {
    PrerequisiteService prerequisiteService;

    @PostMapping
    @Operation(summary = "Add a prerequisite for a subject")
    @PreAuthorize("hasAuthority('PREREQUISITE_MANAGE_SUBJECTS')")
    public ResponseObject<PrerequisiteResponse> create(@RequestBody @Valid PrerequisiteRequest request) {
        return ResponseObject.<PrerequisiteResponse>builder()
                .status(1000)
                .data(prerequisiteService.create(request))
                .message("Prerequisite added successfully")
                .build();
    }

    @GetMapping("/dependents/{subjectId}/dependents")
    @Operation(summary = "Find subjects that depend on this subject",
            description = "Returns a list of subjects that require the given subject as a prerequisite")
    public ResponseObject<List<PrerequisiteResponse>> getDependents(@PathVariable String subjectId) {
        return ResponseObject.<List<PrerequisiteResponse>>builder()
                .status(1000)
                .data(prerequisiteService.getDependents(subjectId))
                .message("Get dependent subjects successfully")
                .build();
    }

    @GetMapping("/{subjectId}/requirements")
    @Operation(summary = "Get all prerequisites that this subject requires")
    public ResponseObject<List<PrerequisiteResponse>> getPrerequisites(@PathVariable String subjectId) {
        return ResponseObject.<List<PrerequisiteResponse>>builder()
                .status(1000)
                .data(prerequisiteService.getPrerequisites(subjectId))
                .message("Get prerequisites successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a prerequisite relationship")
    @PreAuthorize("hasAuthority('PREREQUISITE_MANAGE_SUBJECTS')")
    public ResponseObject<Void> delete(@PathVariable String id) {
        prerequisiteService.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Delete prerequisite successfully")
                .build();
    }
}
