package com.example.smd.mapper;

import com.example.smd.dto.request.PrerequisiteRequest;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.entities.Subject_Prerequisite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PrerequisiteMapper {

    @Mapping(target = "subjectCode", source = "subject.subjectCode")
    @Mapping(target = "subjectName", source = "subject.subjectName")
    @Mapping(target = "prerequisiteSubjectCode", source = "prerequisiteSubject.subjectCode")
    @Mapping(target = "prerequisiteSubjectName", source = "prerequisiteSubject.subjectName")
    PrerequisiteResponse toResponse(Subject_Prerequisite entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "prerequisiteSubject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget Subject_Prerequisite entity, PrerequisiteRequest request);
}
