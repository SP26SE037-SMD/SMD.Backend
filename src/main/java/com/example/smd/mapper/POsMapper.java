package com.example.smd.mapper;

import com.example.smd.dto.request.po.POsCreateRequest;
import com.example.smd.dto.request.po.POsRequest;
import com.example.smd.dto.response.POsResponse;
import com.example.smd.entities.PO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface POsMapper {
    @Mapping(target = "major", ignore = true)
    PO toPo(POsRequest request);

    @Mapping(target = "major", ignore = true) // Sẽ set thủ công trong Service
    @Mapping(target = "poId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PO toPoCreate(POsCreateRequest request);

    POsResponse toPoResponse(PO po);
}
