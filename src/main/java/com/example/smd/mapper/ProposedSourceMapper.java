package com.example.smd.mapper;

import com.example.smd.dto.response.ProposedSourceResponse;
import com.example.smd.entities.ProposedSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProposedSourceMapper {

    @Mapping(target = "id", source = "proposedSourceId")
    @Mapping(target = "sourceCode", source = "source.sourceCode")
    @Mapping(target = "sourceName", source = "source.sourceName")
    @Mapping(target = "subjectCode", source = "subject.subjectCode")
    @Mapping(target = "subjectName", source = "subject.subjectName")
    ProposedSourceResponse toResponse(ProposedSource proposedSource);
}
