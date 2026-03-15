package com.example.smd.mapper;

import com.example.smd.dto.request.clo.CLOsCreateRequest;
import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.request.plo.PLOsCreateRequest;
import com.example.smd.dto.response.clo.CLOsResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.PLOs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CLOsMapper {
    @Mapping(target = "subject", ignore = true) // Chúng ta sẽ set Major thủ công trong Service bằng ID
    @Mapping(target = "cloName", source = "description")
    @Mapping(source = "description", target = "description")
    CLOs toClo(CLOsRequest clOsRequest);

    @Mapping(target = "subject", ignore = true) // Sẽ set thủ công trong Service
    @Mapping(target = "cloId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "description", target = "cloName")
    CLOs toCloCreate(CLOsCreateRequest request);

    CLOsResponse toCloResponse(CLOs clo);
}
