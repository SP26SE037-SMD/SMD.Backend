package com.example.smd.services;

import com.example.smd.dto.request.DepartmentRequest;
import com.example.smd.dto.response.DepartmentResponse;
import com.example.smd.entities.Department;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.DepartmentMapper;
import com.example.smd.repositories.DepartmentRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentService {
    DepartmentRepository departmentRepository;
    DepartmentMapper departmentMapper;

    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByDepartmentCode(request.getDepartmentCode())) {
            throw new AppException(ErrorCode.DEPARTMENT_CODE_EXISTED);
        }
        Department department = departmentMapper.toDepartment(request);
        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Không cho phép đổi mã bộ môn
        if (!department.getDepartmentCode().equals(request.getDepartmentCode())) {
            throw new AppException(ErrorCode.DEPARTMENT_CODE_CANNOT_BE_CHANGED);
        }

        departmentMapper.updateDepartment(department, request);
        return departmentMapper.toDepartmentResponse(departmentRepository.save(department));
    }

    @Transactional // Thêm annotation này ở đây
    public void delete(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // Bây giờ bạn có thể gọi .isEmpty() mà không bị lỗi
        if (!department.getSubjects().isEmpty() || !department.getLecturers().isEmpty()) {
            throw new AppException(ErrorCode.DEPARTMENT_HAS_CONSTRAINTS);
        }

        departmentRepository.delete(department);
    }

    public DepartmentResponse getDetail(UUID id) {
        return departmentRepository.findById(id)
                .map(departmentMapper::toDepartmentResponse) // Dùng :: thay vì dấu chấm
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    public Page<DepartmentResponse> getAll(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departmentCode").ascending());

        if (search == null || search.trim().isEmpty()) {
            return departmentRepository.findAll(pageable).map(departmentMapper::toDepartmentResponse);
        }

        return departmentRepository.findAllByDepartmentNameContainingIgnoreCaseOrDepartmentCodeContainingIgnoreCase(
                search, search, pageable).map(departmentMapper::toDepartmentResponse);
    }
}
