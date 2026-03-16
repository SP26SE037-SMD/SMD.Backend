package com.example.smd.mapper;

import com.example.smd.dto.request.sprint.SprintRequest;
import com.example.smd.dto.response.sprint.SprintResponse;
import com.example.smd.entities.Sprint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SprintMapper {
    @Mapping(target = "sprintId", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    Sprint toSprint(SprintRequest request);

    SprintResponse toSprintResponse(Sprint sprint);

    @Mapping(target = "sprintId", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    void updateSprint(@MappingTarget Sprint sprint, SprintRequest request);
}
