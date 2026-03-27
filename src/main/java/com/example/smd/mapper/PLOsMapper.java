package com.example.smd.mapper;

import com.example.smd.dto.request.plo.PLOsCreateRequest;
import com.example.smd.dto.request.plo.PLOsRequest;
import com.example.smd.dto.request.po.POsCreateRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.PLOs;
import com.example.smd.entities.PO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PLOsMapper {
    @Mapping(source = "description", target = "description")
    PLOs toPlo(PLOsRequest plOsRequest);

    @Mapping(target = "curriculum", ignore = true) // Sẽ set thủ công trong Service
    @Mapping(target = "ploId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PLOs toPloCreate(PLOsCreateRequest request);

    PLOsResponse toPloResponse(PLOs plo);
}
