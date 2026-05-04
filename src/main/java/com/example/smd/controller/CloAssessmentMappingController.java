package com.example.smd.controller;

import com.example.smd.dto.request.CloAssessmentMappingBatchRequest;
import com.example.smd.dto.request.CloAssessmentMappingRequest;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.clo.CloAssessmentMappingResponse;
import com.example.smd.dto.response.validate.AssessmentCloMappingValidationResult;
import com.example.smd.services.CloAssessmentMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clo-assessment-mappings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "CLO-Assessment Mappings", description = "Matrix mapping between CLO and Assessment")
@SecurityRequirement(name = "bearerAuth")
public class CloAssessmentMappingController {

    CloAssessmentMappingService service;

    @PostMapping
//    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Create CLO-Assessment Mapping")
    public ResponseObject<CloAssessmentMappingResponse> create(
            @RequestBody @Valid CloAssessmentMappingRequest request) {
        return ResponseObject.<CloAssessmentMappingResponse>builder()
                .status(1000)
                .data(service.createMapping(request))
                .message("CLO-Assessment mapping created successfully")
                .build();
    }

    @PostMapping("/batch")
//    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Create CLO-Assessment mappings in batch")
    public ResponseObject<List<CloAssessmentMappingResponse>> createBatch(
            @RequestBody @Valid CloAssessmentMappingBatchRequest request) {
        return ResponseObject.<List<CloAssessmentMappingResponse>>builder()
                .status(1000)
                .data(service.createBatch(request))
                .message("CLO-Assessment mappings created successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all mappings for a specific Syllabus")
    public ResponseObject<List<CloAssessmentMappingResponse>> getBySyllabus(@PathVariable String syllabusId) {
        return ResponseObject.<List<CloAssessmentMappingResponse>>builder()
                .status(1000)
                .data(service.getBySyllabus(syllabusId))
                .message("Get syllabus CLO-Assessment mappings successfully")
                .build();
    }

    @GetMapping("/clo/{cloId}")
    @Operation(summary = "Get all Assessments mapped to a specific CLO")
    public ResponseObject<List<CloAssessmentMappingResponse>> getByClo(@PathVariable String cloId) {
        return ResponseObject.<List<CloAssessmentMappingResponse>>builder()
                .status(1000)
                .data(service.getByClo(cloId))
                .message("Get CLO detail mappings successfully")
                .build();
    }

    @GetMapping("/assessment/{assessmentId}")
    @Operation(summary = "Get all CLOs mapped to a specific Assessment")
    public ResponseObject<List<CloAssessmentMappingResponse>> getByAssessment(@PathVariable String assessmentId) {
        return ResponseObject.<List<CloAssessmentMappingResponse>>builder()
                .status(1000)
                .data(service.getByAssessment(assessmentId))
                .message("Get Assessment detail mappings successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MAPPING_DELETE')")
    @Operation(summary = "Remove a CLO-Assessment Mapping")
    public ResponseObject<Void> delete(@PathVariable String id) {
        service.deleteMapping(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("CLO-Assessment mapping deleted successfully")
                .build();
    }

    @PostMapping("/syllabus/{syllabusId}/validate")
    @Operation(summary = "Check a CLO-Assessment Mapping")
    public ResponseObject<AssessmentCloMappingValidationResult> checkMapping(
            @PathVariable("syllabusId") UUID syllabusId,
            @RequestBody List<CloAssessmentMappingRequest> cloAssessmentMappingRequest) {
        return ResponseObject.<AssessmentCloMappingValidationResult>builder()
                .status(1000)
                .data(service.checkMapping(cloAssessmentMappingRequest, syllabusId))
                .message("CLO-Assessment mapping deleted successfully")
                .build();
    }
}
