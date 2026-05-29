package com.example.smd.mapper;

import com.example.smd.dto.request.SystemSettingRequest;
import com.example.smd.dto.response.SystemSettingResponse;
import com.example.smd.entities.System_Setting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {
    SystemSettingResponse toResponse(System_Setting entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "description", ignore = true)
    void updateEntity(@MappingTarget System_Setting entity, SystemSettingRequest request);
}
