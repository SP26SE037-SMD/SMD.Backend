package com.example.smd.mapper;

import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.entities.Syllabus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SyllabusMapper {

    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "subjectName", source = "subject.subjectName")
    @Mapping(target = "subjectCode", source = "subject.subjectCode")
    @Mapping(target = "credit", source = "subject.credits")
    SyllabusResponse toResponse(Syllabus syllabus);

    @Mapping(target = "subject", ignore = true) // Sẽ set thủ công trong Service
    @Mapping(target = "syllabusId", ignore = true)
    Syllabus toSyllabus(SyllabusRequest request);

    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "syllabusId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateSyllabus(@MappingTarget Syllabus syllabus, SyllabusRequest request);
}
