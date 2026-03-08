package com.example.smd.mapper;

import com.example.smd.dto.request.DepartmentRequest;
import com.example.smd.dto.response.DepartmentResponse;
import com.example.smd.entities.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    // Ánh xạ từ Request sang Entity để tạo mới
    Department toDepartment(DepartmentRequest request);

    // Ánh xạ từ Entity sang Response để trả về API
    DepartmentResponse toDepartmentResponse(Department department);

    // Cập nhật Entity từ Request (Dùng cho Put/Patch)
    // @Mapping(target = "departmentCode", ignore = true) đảm bảo mã bộ môn không bị thay đổi
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "departmentCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "subjects", ignore = true)
    void updateDepartment(@MappingTarget Department department, DepartmentRequest request);
}
