package com.example.smd.mapper;

import com.example.smd.dto.request.request.RequestRequest;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Request toRequest(RequestRequest request);

    RequestResponse toRequestResponse(Request request);

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateRequest(@MappingTarget Request request, RequestRequest requestDto);
}
