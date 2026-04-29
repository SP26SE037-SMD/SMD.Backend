package com.example.smd.controller;

import com.example.smd.dto.request.CloPloMappingBulkRequest;
import com.example.smd.dto.request.CloPloMappingRequest;
import com.example.smd.dto.request.PoPloMappingRequest;
import com.example.smd.dto.response.clo.CloPloMappingBulkResponse;
import com.example.smd.dto.response.clo.CloPloMappingResponse;
import com.example.smd.dto.response.ResponseObject;

import com.example.smd.dto.response.validate.CloPloMappingCheckResponse;
import com.example.smd.dto.response.validate.PoPloMappingCheckResponse;
import com.example.smd.services.CloPloMappingService;
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
@RequestMapping("/api/clo-plo-mappings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "CLO-PLO Mappings", description = "Matrix mapping between CLO and PLO")
@SecurityRequirement(name = "bearerAuth")
public class CloPloMappingController {
    CloPloMappingService service;

    @PostMapping
    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Create CLO-PLO Mapping")
    public ResponseObject<CloPloMappingResponse> create(@RequestBody @Valid CloPloMappingRequest request) {
        return ResponseObject.<CloPloMappingResponse>builder()
                .status(1000)
                .data(service.createMapping(request))
                .message("Mapping created successfully")
                .build();
    }

    @PostMapping("/bulk-configure")
    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(summary = "Bulk configure CLO-PLO mappings")
    public ResponseObject<CloPloMappingBulkResponse> bulkConfigure(@RequestBody @Valid CloPloMappingBulkRequest request) {
        return ResponseObject.<CloPloMappingBulkResponse>builder()
                .status(1000)
                .data(service.bulkConfigureMappings(request))
                .message("CLO-PLO mappings configured successfully")
                .build();
    }

    @GetMapping("/subject/{subjectId}/curriculum/{curriculumId}")
    @Operation(summary = "Get all mappings for a specific Subject and Curiculum")
    public ResponseObject<List<CloPloMappingResponse>> getBySubject(@PathVariable UUID subjectId, @PathVariable UUID curriculumId) {
        return ResponseObject.<List<CloPloMappingResponse>>builder()
                .status(1000)
                .data(service.getFullMappingDetails(subjectId, curriculumId))
                .message("Get subject mappings successfully")
                .build();
    }

    @GetMapping("/clo/{cloId}")
    @Operation(summary = "Get all PLOs mapped to a specific CLO")
    public ResponseObject<List<CloPloMappingResponse>> getByClo(@PathVariable String cloId) {
        return ResponseObject.<List<CloPloMappingResponse>>builder()
                .status(1000)
                .data(service.getByClo(cloId))
                .message("Get CLO detail mapping successfully")
                .build();
    }

    @GetMapping("/plo/{ploId}")
    @Operation(summary = "Get all CLOs contributing to a specific PLO")
    public ResponseObject<List<CloPloMappingResponse>> getByPlo(@PathVariable String ploId) {
        return ResponseObject.<List<CloPloMappingResponse>>builder()
                .status(1000)
                .data(service.getByPlo(ploId))
                .message("Get PLO detail mapping successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MAPPING_UPDATE')")
    @Operation(summary = "Update contribution level (Low/Medium/High)")
    public ResponseObject<CloPloMappingResponse> update(@PathVariable String id, @RequestParam String level) {
        return ResponseObject.<CloPloMappingResponse>builder()
                .status(1000)
                .data(service.updateLevel(id, level))
                .message("Mapping updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MAPPING_DELETE')")
    public ResponseObject<Void> delete(@PathVariable String id) {
        service.deleteMapping(id);
        return ResponseObject.<Void>builder().status(1000).message("Deleted successfully").build();
    }

    @PostMapping("/curriculum/{curriculumId}/subject/{subjectId}/validate")
    public ResponseObject<CloPloMappingCheckResponse> validatePo(
            @RequestBody List<CloPloMappingRequest> request,
            @PathVariable("curriculumId") UUID curriculumId,
            @PathVariable("subjectId") UUID subjectId
    ) {
        var response = service.checkMapping(request, curriculumId, subjectId);
        return ResponseObject.<CloPloMappingCheckResponse>builder()
                .status(1000)
                .data(response)
                .message("CLO-PLO-Mapping validate successfully")
                .build();
    }
}
