package com.example.smd.services;

import com.example.smd.dto.request.PermissionRequest;
import com.example.smd.dto.response.PermissionResponse;
import com.example.smd.entities.Permission;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PermissionMapper;
import com.example.smd.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    // GetAll quyền có phân trang
    public Page<PermissionResponse> getAllPermissions(String search, int page, int size, String[] sort) {
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

        // 2. Lấy tất cả permission và map sang DTO
        return permissionRepository.findAll(pagingSort)
                .map(permissionMapper::toResponse);
    }

    // Lấy chi tiết quyền theo ID
    public PermissionResponse getPermissionById(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toResponse(permission);
    }

    // Lấy chi tiết quyền theo tên
    public PermissionResponse getPermissionByName(String permissionName) {
        Permission permission = permissionRepository.findByPermissionName(permissionName)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toResponse(permission);
    }

    // Tạo quyền mới
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new AppException(ErrorCode.PERMISSION_EXISTS);
        }

        Permission permission = permissionMapper.toEntity(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toResponse(permission);
    }

    // Cập nhật quyền
    @Transactional
    public PermissionResponse updatePermission(UUID permissionId, PermissionRequest request) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        if (request.getPermissionName() != null &&
                !request.getPermissionName().equals(permission.getPermissionName()) &&
                permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new AppException(ErrorCode.PERMISSION_EXISTS);
        }

        permissionMapper.updateEntity(permission, request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toResponse(permission);
    }

    // Xóa quyền
    @Transactional
    public boolean deletePermission(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));

        permission.getRoles().clear();
        permissionRepository.save(permission);
        permissionRepository.deleteById(permissionId);
        return true;
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
