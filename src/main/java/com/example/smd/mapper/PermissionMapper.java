package com.example.smd.mapper;

import com.example.smd.dto.request.PermissionRequest;
import com.example.smd.dto.response.PermissionResponse;
import com.example.smd.entities.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    // Chuyển đổi từ Entity Permission sang DTO PermissionResponse
    public PermissionResponse toResponse(Permission permission) {
        if (permission == null) {
            return null;
        }

        return PermissionResponse.builder()
                .permissionId(permission.getPermissionId())
                .permissionName(permission.getPermissionName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .build();
    }

    // Chuyển đổi từ DTO PermissionRequest sang Entity Permission
    public Permission toEntity(PermissionRequest request) {
        if (request == null) {
            return null;
        }

        return Permission.builder()
                .permissionName(request.getPermissionName())
                .description(request.getDescription())
                .build();
    }

    // Cập nhật thông tin Entity Permission từ DTO PermissionRequest
    public void updateEntity(Permission permission, PermissionRequest request) {
        if (request.getPermissionName() != null) {
            permission.setPermissionName(request.getPermissionName());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
    }
}
