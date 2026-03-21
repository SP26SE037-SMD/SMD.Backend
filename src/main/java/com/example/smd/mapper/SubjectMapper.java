package com.example.smd.mapper;

import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.entities.Subject;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubjectMapper {

    // Chuyển từ Request DTO sang Entity để lưu vào DB
    @Mapping(target = "subjectId", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "department", ignore = true)
    Subject toSubject(SubjectRequest request);

    // Chuyển từ Entity sang Response DTO để trả về cho Client
    @Mapping(target = "department", source = "department")
    SubjectResponse toSubjectResponse(Subject subject);

    // Cập nhật Entity từ Request DTO
    @Mapping(target = "subjectId", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Không cho phép update ngày tạo
    @Mapping(target = "department", ignore = true)
    void updateSubject(@MappingTarget Subject subject, SubjectRequest request);
}
