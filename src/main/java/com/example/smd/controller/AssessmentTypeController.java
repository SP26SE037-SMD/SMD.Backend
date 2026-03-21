package com.example.smd.controller;

import com.example.smd.dto.request.AssessmentTypeRequest;
import com.example.smd.dto.response.AssessmentTypeResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AssessmentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Assessment Type", description = "Assessment Type Management APIs")
@RestController
@RequestMapping("/api/assessment-types")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssessmentTypeController {

    private final AssessmentTypeService assessmentTypeService;

    @GetMapping
    @Operation(summary = "Get all assessment types with pagination")
    public ResponseObject<PagedResponse<AssessmentTypeResponse>> getAllTypes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "typeName,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<AssessmentTypeResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(assessmentTypeService.getAllTypes(search, page, size, sort)))
                .message("Get all assessment types successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment type by ID")
    public ResponseObject<AssessmentTypeResponse> getTypeById(@PathVariable String id) {
        return ResponseObject.<AssessmentTypeResponse>builder()
                .status(1000)
                .data(assessmentTypeService.getTypeById(id))
                .message("Get assessment type successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create assessment type")
    public ResponseObject<AssessmentTypeResponse> createType(@Valid @RequestBody AssessmentTypeRequest request) {
        return ResponseObject.<AssessmentTypeResponse>builder()
                .status(1000)
                .data(assessmentTypeService.createType(request))
                .message("Create assessment type successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update assessment type")
    public ResponseObject<AssessmentTypeResponse> updateType(
            @PathVariable String id,
            @Valid @RequestBody AssessmentTypeRequest request
    ) {
        return ResponseObject.<AssessmentTypeResponse>builder()
                .status(1000)
                .data(assessmentTypeService.updateType(id, request))
                .message("Update assessment type successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete assessment type")
    public ResponseObject<Boolean> deleteType(@PathVariable String id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(assessmentTypeService.deleteType(id))
                .message("Delete assessment type successfully")
                .build();
    }
}
