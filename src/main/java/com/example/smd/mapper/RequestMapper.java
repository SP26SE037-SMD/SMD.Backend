package com.example.smd.mapper;

import com.example.smd.dto.request.request.RequestCreateRequest;
import com.example.smd.dto.request.request.RequestUpdateRequest;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.entities.Request;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RequestMapper {

    // ---- Entity <- Request ----

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Request toEntity(RequestCreateRequest request);

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "targetId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Request request, RequestUpdateRequest dto);

    // ---- Response <- Entity ----

    @Mapping(target = "createdBy.accountId", source = "createdBy.accountId")
    @Mapping(target = "createdBy.email",     source = "createdBy.email")
    @Mapping(target = "createdBy.fullName",  source = "createdBy.fullName")
    @Mapping(target = "receivedBy.accountId", source = "receivedBy.accountId")
    @Mapping(target = "receivedBy.email",     source = "receivedBy.email")
    @Mapping(target = "receivedBy.fullName",  source = "receivedBy.fullName")
    RequestResponse toResponse(Request request);
}
