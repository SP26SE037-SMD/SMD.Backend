package com.example.smd.mapper;

import com.example.smd.dto.request.RoleRequest;
import com.example.smd.dto.response.RoleResponse;
import com.example.smd.entities.Role;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public RoleMapper(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }

        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .permissions(role.getPermissions() != null
                        ? role.getPermissions().stream()
                        .map(permissionMapper::toResponse)
                        .collect(Collectors.toSet())
                        : null)
                .createdAt(role.getCreatedAt())
                .build();
    }

    public Role toEntity(RoleRequest request) {
        if (request == null) {
            return null;
        }

        return Role.builder()
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Role role, RoleRequest request) {
        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
    }
}
