package com.example.smd.controller;

import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SourceResponse;
import com.example.smd.services.SyllabusSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syllabus-sources")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Syllabus Source", description = "Management of the relationship between Syllabuses and Reference Sources")
@SecurityRequirement(name = "bearerAuth")
public class SyllabusSourceController {
    SyllabusSourceService service;

    @PostMapping("/{syllabusId}/sources")
    @Operation(
            summary = "Assign sources to a syllabus",
            description = "Takes a list of Source IDs and links them to a specific Syllabus. Throws an error if the mapping already exists."
    )
    public ResponseObject<Void> addSources(
            @PathVariable UUID syllabusId,
            @RequestBody List<UUID> sourceIds,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        service.addSourcesToSyllabus(syllabusId, sourceIds, userId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Sources assigned to syllabus successfully")
                .build();
    }

    @GetMapping("/{syllabusId}")
    @Operation(
            summary = "Get all sources for a syllabus",
            description = "Retrieves a complete list of reference sources associated with a given Syllabus ID."
    )
    public ResponseObject<List<SourceResponse>> getSources(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<SourceResponse>>builder()
                .status(1000)
                .data(service.getSourcesBySyllabusId(syllabusId))
                .message("Retrieved syllabus sources successfully")
                .build();
    }

    @DeleteMapping("/{syllabusId}/sources/{sourceId}")
    @Operation(
            summary = "Remove a source from a syllabus",
            description = "Deletes the link between a Syllabus and a Source. Note: This does not delete the actual Source record from the system."
    )
    public ResponseObject<Void> removeSource(
            @PathVariable UUID syllabusId,
            @PathVariable UUID sourceId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        service.removeSourceFromSyllabus(syllabusId, sourceId, userId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Source unlinked from syllabus successfully")
                .build();
    }
}