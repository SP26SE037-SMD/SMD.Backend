package com.example.smd.mapper;

import com.example.smd.dto.request.SystemLogRequest;
import com.example.smd.dto.response.SystemLogResponse;
import com.example.smd.entities.System_Log;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemLogMapper {

    // Ánh xạ từ Entity sang Response
    @Mapping(source = "account.accountId", target = "accountId")
    @Mapping(source = "account.email", target = "accountEmail")
    @Mapping(source = "account.fullName", target = "accountFullName")
    @Mapping(source = "targetId", target = "targetId")
    SystemLogResponse toSystemLogResponse(System_Log systemLog);

    // Ánh xạ từ Request sang Entity (account sẽ set trong service)
    @Mapping(target = "logId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "targetId", target = "targetId")
    System_Log toSystemLog(SystemLogRequest request);
}
