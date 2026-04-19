package com.example.smd.mapper;

import com.example.smd.dto.response.clo.CloPloMappingResponse;
import com.example.smd.entities.CLO_PLO_Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CloPloMappingMapper {
    @Mapping(target = "cloId", source = "clo.cloId")
    @Mapping(target = "cloCode", source = "clo.cloCode")
    @Mapping(target = "cloDescription", source = "clo.description")
    @Mapping(target = "ploId", source = "plo.ploId")
    @Mapping(target = "ploCode", source = "plo.ploCode")
    @Mapping(target = "ploDescription", source = "plo.description")
    CloPloMappingResponse toResponse(CLO_PLO_Mapping entity);
}
