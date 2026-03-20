package com.example.smd.controller;

import com.example.smd.dto.request.PermissionRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.PermissionResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.permission.ImportPermissionResponse;
import com.example.smd.services.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Permission", description = "Permission Management APIs")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final PermissionService permissionService;

    // API lấy danh sách quyền có phân trang
    @GetMapping
    @Operation(summary = "Get all permissions with pagination")
    public ResponseObject<PagedResponse<PermissionResponse>> getAllPermissions(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "permissionName,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<PermissionResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(permissionService.getAllPermissions(search, page, size, sort)))
                .message("Get all permissions successfully")
                .build();
    }

    // API lấy chi tiết quyền theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Get permission by ID")
    public ResponseObject<PermissionResponse> getPermissionById(@PathVariable UUID id) {
        return ResponseObject.<PermissionResponse>builder()
                .status(1000)
                .data(permissionService.getPermissionById(id))
                .message("Get permission successfully")
                .build();
    }

    // API lấy chi tiết quyền theo tên
    @GetMapping("/name/{name}")
//    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Get permission by name")
    public ResponseObject<PermissionResponse> getPermissionByName(@PathVariable String name) {
        return ResponseObject.<PermissionResponse>builder()
                .status(1000)
                .data(permissionService.getPermissionByName(name))
                .message("Get permission successfully")
                .build();
    }

    // API tạo quyền mới
    @PostMapping
//    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    @Operation(summary = "Create new permission")
    public ResponseObject<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        return ResponseObject.<PermissionResponse>builder()
                .status(1000)
                .data(permissionService.createPermission(request))
                .message("Create permission successfully")
                .build();
    }

    // API cập nhật quyền
    @PutMapping("/{id}")
//    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    @Operation(summary = "Update permission")
    public ResponseObject<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody PermissionRequest request) {
        return ResponseObject.<PermissionResponse>builder()
                .status(1000)
                .data(permissionService.updatePermission(id, request))
                .message("Update permission successfully")
                .build();
    }

    // API xóa quyền
    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    @Operation(summary = "Delete permission")
    public ResponseObject<Boolean> deletePermission(@PathVariable UUID id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(permissionService.deletePermission(id))
                .message("Delete permission successfully")
                .build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import permissions from Excel")
    public ResponseObject<ImportPermissionResponse> importPermissions(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseObject.<ImportPermissionResponse>builder()
                .status(1000)
                .data(permissionService.importPermissions(file))
                .message("Import permissions successfully")
                .build();
    }
}
