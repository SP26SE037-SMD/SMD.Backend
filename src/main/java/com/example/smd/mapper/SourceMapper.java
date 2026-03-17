package com.example.smd.mapper;

import com.example.smd.dto.request.SourceRequest;
import com.example.smd.dto.response.SourceResponse;
import com.example.smd.entities.Source;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SourceMapper {
    @Mapping(target = "type", ignore = true)
    Source toSource(SourceRequest request);
    SourceResponse toResponse(Source source);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSource(@MappingTarget Source source, SourceRequest request);
}