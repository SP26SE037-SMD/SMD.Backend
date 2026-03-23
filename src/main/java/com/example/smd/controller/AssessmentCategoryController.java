package com.example.smd.controller;

import com.example.smd.dto.request.AssessmentCategoryRequest;
import com.example.smd.dto.response.AssessmentCategoryResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AssessmentCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Assessment Category", description = "Assessment Category Management APIs")
@RestController
@RequestMapping("/api/assessment-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssessmentCategoryController {

    private final AssessmentCategoryService assessmentCategoryService;

    @GetMapping
    @Operation(summary = "Get all assessment categories with pagination")
    public ResponseObject<PagedResponse<AssessmentCategoryResponse>> getAllCategories(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "categoryName,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<AssessmentCategoryResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(assessmentCategoryService.getAllCategories(search, page, size, sort)))
                .message("Get all assessment categories successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment category by ID")
        public ResponseObject<AssessmentCategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseObject.<AssessmentCategoryResponse>builder()
                .status(1000)
                .data(assessmentCategoryService.getCategoryById(id))
                .message("Get assessment category successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create assessment category")
    public ResponseObject<AssessmentCategoryResponse> createCategory(@Valid @RequestBody AssessmentCategoryRequest request) {
        return ResponseObject.<AssessmentCategoryResponse>builder()
                .status(1000)
                .data(assessmentCategoryService.createCategory(request))
                .message("Create assessment category successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update assessment category")
    public ResponseObject<AssessmentCategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody AssessmentCategoryRequest request
    ) {
        return ResponseObject.<AssessmentCategoryResponse>builder()
                .status(1000)
                .data(assessmentCategoryService.updateCategory(id, request))
                .message("Update assessment category successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete assessment category")
        public ResponseObject<Boolean> deleteCategory(@PathVariable UUID id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(assessmentCategoryService.deleteCategory(id))
                .message("Delete assessment category successfully")
                .build();
    }
}
