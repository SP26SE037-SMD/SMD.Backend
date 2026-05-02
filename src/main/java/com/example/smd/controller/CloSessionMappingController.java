package com.example.smd.controller;

import com.example.smd.dto.request.CloSessionMappingBatchRequest;
import com.example.smd.dto.request.CloSessionMappingRequest;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.clo.CloSessionMappingResponse;
import com.example.smd.dto.response.validate.SessionCloMappingValidationResult;
import com.example.smd.services.CloSessionMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clo-session-mappings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "CLO-Session Mappings", description = "Matrix mapping between CLO and Session")
@SecurityRequirement(name = "bearerAuth")
public class CloSessionMappingController {

    CloSessionMappingService service;

    @PostMapping
//    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Create CLO-Session Mapping")
    public ResponseObject<CloSessionMappingResponse> create(
            @RequestBody @Valid CloSessionMappingRequest request) {
        return ResponseObject.<CloSessionMappingResponse>builder()
                .status(1000)
                .data(service.createMapping(request))
                .message("CLO-Session mapping created successfully")
                .build();
    }

    @PostMapping("/batch")
//    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Create CLO-Session mappings in batch")
    public ResponseObject<List<CloSessionMappingResponse>> createBatch(
            @RequestBody @Valid CloSessionMappingBatchRequest request) {
        return ResponseObject.<List<CloSessionMappingResponse>>builder()
                .status(1000)
                .data(service.createBatch(request))
                .message("CLO-Session mappings created successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all mappings for a specific Syllabus")
    public ResponseObject<List<CloSessionMappingResponse>> getBySyllabus(@PathVariable String syllabusId) {
        return ResponseObject.<List<CloSessionMappingResponse>>builder()
                .status(1000)
                .data(service.getBySyllabus(syllabusId))
                .message("Get syllabus CLO-Session mappings successfully")
                .build();
    }

    @GetMapping("/clo/{cloId}")
    @Operation(summary = "Get all Sessions mapped to a specific CLO")
    public ResponseObject<List<CloSessionMappingResponse>> getByClo(@PathVariable String cloId) {
        return ResponseObject.<List<CloSessionMappingResponse>>builder()
                .status(1000)
                .data(service.getByClo(cloId))
                .message("Get CLO detail mappings successfully")
                .build();
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get all CLOs mapped to a specific Session")
    public ResponseObject<List<CloSessionMappingResponse>> getBySession(@PathVariable String sessionId) {
        return ResponseObject.<List<CloSessionMappingResponse>>builder()
                .status(1000)
                .data(service.getBySession(sessionId))
                .message("Get Session detail mappings successfully")
                .build();
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('MAPPING_DELETE')")
    @Operation(summary = "Remove a CLO-Session Mapping")
    public ResponseObject<Void> delete(@PathVariable String id) {
        service.deleteMapping(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("CLO-Session mapping deleted successfully")
                .build();
    }

    @PostMapping("/syllabus/{syllabusId}/validate")
    @Operation(summary = "Check a CLO-Session Mapping")
    public ResponseObject<SessionCloMappingValidationResult> checkMapping(
            @PathVariable("syllabusId") UUID syllabusId,
            @RequestBody List<CloSessionMappingRequest> cloSessionMappingRequest) {
        return ResponseObject.<SessionCloMappingValidationResult>builder()
                .status(1000)
                .data(service.checkMapping(cloSessionMappingRequest, syllabusId))
                .message("CLO-Session mapping deleted successfully")
                .build();
    }
}
