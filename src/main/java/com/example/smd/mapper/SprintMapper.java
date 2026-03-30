package com.example.smd.mapper;

import com.example.smd.dto.request.sprint.SprintCreateRequest;
import com.example.smd.dto.request.sprint.SprintUpdateRequest;
import com.example.smd.dto.response.sprint.SprintResponse;
import com.example.smd.entities.Sprint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SprintMapper {
    @Mapping(target = "sprintId", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    Sprint toSprint(SprintCreateRequest request);

    @Mapping(target = "accountId", source = "account.accountId")
    @Mapping(target = "curriculumId", source = "curriculum.curriculumId")
    SprintResponse toSprintResponse(Sprint sprint);

    @Mapping(target = "sprintId", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    void updateSprint(@MappingTarget Sprint sprint, SprintUpdateRequest request);
}
