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

    // GetAll vai trò có phân trang
    public Page<RoleResponse> getAllRoles(String search, int page, int size, String[] sort) {
        // 1. Xử lý sắp xếp (Sắp xếp theo field CamelCase của Java)
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

        // 2. Lấy tất cả role và map sang DTO
        return roleRepository.findAll(pagingSort)
                .map(roleMapper::toResponse);
    }

    // Lấy chi tiết vai trò theo ID
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    // Lấy chi tiết vai trò theo tên
    public RoleResponse getRoleByName(String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    // Tạo vai trò mới
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_EXISTS);
        }

        Role role = roleMapper.toEntity(request);

        // Gán danh sách permission cho role nếu có
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

    // Cập nhật vai trò
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

        // Cập nhật lại danh sách permission nếu có (chỉ thêm mới, không xóa cũ)
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (String permissionId : request.getPermissionIds()) {
                var convert = UUID.fromString(permissionId);
                Permission permission = permissionRepository.findById(convert)
                        .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
                role.getPermissions().add(permission); // Chỉ thêm, Set sẽ tự động loại bỏ trùng lặp
            }
        }

        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    // Xóa danh sách permission khỏi role
    @Transactional
    public RoleResponse removePermissionsFromRole(UUID roleId, List<String> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new AppException(ErrorCode.PERMISSION_LIST_REQUIRED);
        }

        // Xóa từng permission khỏi role
        for (String permissionId : permissionIds) {
            var convert = UUID.fromString(permissionId);
            Permission permission = permissionRepository.findById(convert)
                    .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
            role.getPermissions().remove(permission);
        }

        role = roleRepository.save(role);
        log.info("Removed {} permissions from role: {}", permissionIds.size(), role.getRoleName());
        return roleMapper.toResponse(role);
    }

    // Helper method để xử lý hướng sắp xếp
    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
