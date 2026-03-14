package com.example.smd.mapper;

import com.example.smd.dto.request.POsRequest;
import com.example.smd.dto.response.POsResponse;
import com.example.smd.entities.PO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface POsMapper {
    @Mapping(target = "major", ignore = true)
    PO toPo(POsRequest request);

    @Mapping(target = "majorId", source = "major.majorId")
    POsResponse toPoResponse(PO po);
}
