package com.example.smd.controller;

import com.example.smd.dto.request.DepartmentRequest;
import com.example.smd.dto.response.DepartmentResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Department", description = "Endpoints for managing academic departments")
@SecurityRequirement(name = "bearerAuth")

public class DepartmentController {
    DepartmentService departmentService;

    @PostMapping
    @Operation(summary = "Create a new department")
    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    public ResponseObject<DepartmentResponse> create(@RequestBody @Valid DepartmentRequest request) {
        return ResponseObject.<DepartmentResponse>builder()
                .status(1000)
                .data(departmentService.create(request))
                .message("Create department successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all departments with pagination and search")
    public ResponseObject<PagedResponse<DepartmentResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<DepartmentResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(departmentService.getAll(search, page, size)))
                .message("Get all departments successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department details by ID")
    public ResponseObject<DepartmentResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<DepartmentResponse>builder()
                .status(1000)
                .data(departmentService.getDetail(id))
                .message("Get department detail successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department information")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    public ResponseObject<DepartmentResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid DepartmentRequest request) {
        return ResponseObject.<DepartmentResponse>builder()
                .status(1000)
                .data(departmentService.update(id, request))
                .message("Update department successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a department")
    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Delete department successfully")
                .build();
    }
}