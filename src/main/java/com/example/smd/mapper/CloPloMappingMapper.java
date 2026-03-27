package com.example.smd.mapper;

import com.example.smd.dto.response.clo.CloPloMappingResponse;
import com.example.smd.entities.CLO_PLO_Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CloPloMappingMapper {
    @Mapping(target = "cloId", source = "clo.cloId")
    @Mapping(target = "ploId", source = "plo.ploId")
    CloPloMappingResponse toResponse(CLO_PLO_Mapping entity);
}
