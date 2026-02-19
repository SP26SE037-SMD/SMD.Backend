package com.example.smd.services;

import com.example.smd.dto.request.RoleRequest;
import com.example.smd.dto.response.RoleResponse;
import com.example.smd.entities.Permission;
import com.example.smd.entities.Role;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.RoleMapper;
import com.example.smd.repositories.PermissionRepository;
import com.example.smd.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public Page<RoleResponse> getAllRoles(String search, int page, int size, String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        return roleRepository.findAll(pagingSort)
                .map(roleMapper::toResponse);
    }

    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    public RoleResponse getRoleByName(String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_EXISTS);
        }

        Role role = roleMapper.toEntity(request);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permissionId : request.getPermissionIds()) {
                var convert = UUID.fromString(permissionId);
                Permission permission = permissionRepository.findById(convert)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
                permissions.add(permission);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID roleId, RoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (request.getRoleName() != null &&
                !request.getRoleName().equals(role.getRoleName()) &&
                roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_EXISTS);
        }

        roleMapper.updateEntity(role, request);

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (String permissionId : request.getPermissionIds()) {
                var convert = UUID.fromString(permissionId);
                Permission permission = permissionRepository.findById(convert)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
                permissions.add(permission);
            }
            role.getPermissions().clear();
            role.getPermissions().addAll(permissions);
        }

        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
