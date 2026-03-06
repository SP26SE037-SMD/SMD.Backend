package com.example.smd.controller;

import com.example.smd.dto.request.RoleRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.RoleResponse;
import com.example.smd.services.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Role", description = "Role Management APIs")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    // API lấy danh sách vai trò có phân trang và tìm kiếm
    @GetMapping
    @Operation(summary = "Get all roles with pagination and search")
    public ResponseObject<PagedResponse<RoleResponse>> getAllRoles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "roleName,asc") String[] sort
    ) {
        Page<RoleResponse> roles = roleService.getAllRoles(search, page, size, sort);
        return ResponseObject.<PagedResponse<RoleResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(roles))
                .message("Get all roles successfully")
                .build();
    }

    // API lấy chi tiết vai trò theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseObject<RoleResponse> getRoleById(@PathVariable String id) {
        var convert = UUID.fromString(id);
        return ResponseObject.<RoleResponse>builder()
                .status(1000)
                .data(roleService.getRoleById(convert))
                .message("Get role successfully")
                .build();
    }

    // API lấy chi tiết vai trò theo tên
    @GetMapping("/name/{name}")
    @Operation(summary = "Get role by name")
    public ResponseObject<RoleResponse> getRoleByName(@PathVariable String name) {
        return ResponseObject.<RoleResponse>builder()
                .status(1000)
                .data(roleService.getRoleByName(name))
                .message("Get role successfully")
                .build();
    }

    // API tạo vai trò mới
    @PostMapping
    @Operation(summary = "Create new role")
    public ResponseObject<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseObject.<RoleResponse>builder()
                .status(1000)
                .data(roleService.createRole(request))
                .message("Create role successfully")
                .build();
    }

    // API cập nhật vai trò theo ID
    @PutMapping("/{id}")
    @Operation(summary = "Update role by ID")
    public ResponseObject<RoleResponse> updateRole(
            @PathVariable String id,
            @Valid @RequestBody RoleRequest request) {
        var convert = UUID.fromString(id);
        return ResponseObject.<RoleResponse>builder()
                .status(1000)
                .data(roleService.updateRole(convert, request))
                .message("Update role successfully")
                .build();
    }

    // API xóa danh sách permission khỏi role
    @DeleteMapping("/{id}/permissions")
    @Operation(summary = "Remove permissions from role")
    public ResponseObject<RoleResponse> removePermissionsFromRole(
            @PathVariable String id,
            @RequestBody List<String> permissionIds) {
        var convert = UUID.fromString(id);
        return ResponseObject.<RoleResponse>builder()
                .status(1000)
                .data(roleService.removePermissionsFromRole(convert, permissionIds))
                .message("Remove permissions from role successfully")
                .build();
    }

}
