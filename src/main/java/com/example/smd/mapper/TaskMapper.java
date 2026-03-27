package com.example.smd.mapper;

import com.example.smd.dto.request.task.TaskRequest;
import com.example.smd.dto.request.task.TaskItemRequest;
import com.example.smd.dto.response.task.TaskListResponse;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.entities.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    Task toTask(TaskRequest request);

    @Mapping(target = "sprintId", source = "sprint.sprintId")
    @Mapping(target = "accountId", source = "account.accountId")
    @Mapping(target = "syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "curriculumId", source = "curriculum.curriculumId")
    TaskResponse toTaskResponse(Task task);

    @Mapping(target = "sprintId", source = "sprint.sprintId")
    @Mapping(target = "account.accountId", source = "account.accountId")
    @Mapping(target = "account.email", source = "account.email")
    @Mapping(target = "account.fullName", source = "account.fullName")
    @Mapping(target = "syllabus.syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "syllabus.syllabusName", source = "syllabus.syllabusName")
    @Mapping(target = "curriculumId", source = "curriculum.curriculumId")
    TaskListResponse toTaskListResponse(Task task);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    Task toTask(TaskItemRequest request);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "curriculum", ignore = true)
    void updateTask(@MappingTarget Task task, TaskRequest request);
}
