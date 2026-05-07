package com.example.smd.mapper;

import com.example.smd.dto.request.taskV2.TaskV2CreateRequest;
import com.example.smd.dto.request.taskV2.TaskV2CreateVPRequest;
import com.example.smd.dto.request.taskV2.TaskV2UpdateRequest;
import com.example.smd.dto.response.TaskV2Response;
import com.example.smd.entities.TaskV2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TaskV2Mapper {

    // ---- Entity <- Request ----

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TaskV2 toEntity(TaskV2CreateRequest request);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    TaskV2 requestVPtoEntity(TaskV2CreateVPRequest request);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget TaskV2 task, TaskV2UpdateRequest request);

    // ---- Response <- Entity ----

    @Mapping(target = "sprintId", source = "sprint.sprintId")
    @Mapping(target = "assignTo.accountId", source = "account.accountId")
    @Mapping(target = "assignTo.email", source = "account.email")
    @Mapping(target = "assignTo.fullName", source = "account.fullName")
    @Mapping(target = "createdBy.accountId", source = "createdBy.accountId")
    @Mapping(target = "createdBy.email", source = "createdBy.email")
    @Mapping(target = "createdBy.fullName", source = "createdBy.fullName")
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    @Mapping(target = "document", ignore = true)
    TaskV2Response toResponse(TaskV2 task);
}
