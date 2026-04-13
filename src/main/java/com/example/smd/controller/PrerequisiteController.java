package com.example.smd.controller;

import com.example.smd.dto.request.PrerequisiteRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.prerequisite.ImportPrerequisiteResponse;
import com.example.smd.services.PrerequisiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prerequisites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Prerequisite", description = "Endpoints for managing subject prerequisites")
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping("/dependents/code/{subjectCode}/dependents")
    @Operation(summary = "Find subjects that depend on this subject by code",
            description = "Returns a list of subjects that require the given subject code as a prerequisite")
    public ResponseObject<List<PrerequisiteResponse>> getDependentsByCode(@PathVariable String subjectCode) {
        return ResponseObject.<List<PrerequisiteResponse>>builder()
                .status(1000)
                .data(prerequisiteService.getDependentsByCode(subjectCode))
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

    @GetMapping("/code/{subjectCode}/requirements")
    @Operation(summary = "Get all prerequisites that this subject requires by code")
    public ResponseObject<List<PrerequisiteResponse>> getPrerequisitesByCode(@PathVariable String subjectCode) {
        return ResponseObject.<List<PrerequisiteResponse>>builder()
                .status(1000)
                .data(prerequisiteService.getPrerequisitesByCode(subjectCode))
                .message("Get prerequisites successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a prerequisite relationship")
    @PreAuthorize("hasAuthority('PREREQUISITE_MANAGE_SUBJECTS')")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        prerequisiteService.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Delete prerequisite successfully")
                .build();
    }

    @PostMapping(value = "/import", consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import prerequisites from Excel")
    public ResponseObject<ImportPrerequisiteResponse> importPrerequisites(@RequestParam("file") MultipartFile file) {
        return ResponseObject.<ImportPrerequisiteResponse>builder()
                .status(1000)
                .data(prerequisiteService.importPrerequisites(file))
                .message("Import prerequisites successfully")
                .build();
    }
}
