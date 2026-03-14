package com.example.smd.mapper;

import com.example.smd.dto.response.PoPloMappingResponse;
import com.example.smd.entities.PO_PLO_Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PoPloMappingMapper {
    @Mapping(target = "poId", source = "po.poId")
    @Mapping(target = "poCode", source = "po.poCode")
    @Mapping(target = "ploId", source = "plo.ploId")
    @Mapping(target = "ploCode", source = "plo.ploCode")
    PoPloMappingResponse toResponse(PO_PLO_Mapping mapping);
}
