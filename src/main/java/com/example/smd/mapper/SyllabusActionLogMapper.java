package com.example.smd.mapper;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.response.SyllabusActionLogResponse;
import com.example.smd.entities.Syllabus_Action_Logs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SyllabusActionLogMapper {
    @Mapping(target = "syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "actionByFullName", source = "actionBy.fullName")
    @Mapping(target = "actionType", source = "action")
    SyllabusActionLogResponse toResponse(Syllabus_Action_Logs log);

    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "actionBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Syllabus_Action_Logs toEntity(SyllabusActionLogRequest request);
}
