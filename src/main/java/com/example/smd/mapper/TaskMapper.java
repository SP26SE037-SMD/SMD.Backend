package com.example.smd.mapper;

import com.example.smd.dto.request.task.TaskCreateRequest;
import com.example.smd.dto.request.task.TaskUpdateRequest;
import com.example.smd.dto.request.task.TaskVPRequest;
import com.example.smd.dto.response.task.TaskListResponse;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.dto.response.task.TaskVPResponse;
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
    @Mapping(target = "subject", ignore = true)
    Task toTask(TaskCreateRequest request);

    @Mapping(target = "sprintId", source = "sprint.sprintId")
    @Mapping(target = "accountId", source = "account.accountId")
    @Mapping(target = "syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "subjectStatus", source = "subject.status")
    TaskResponse toTaskResponse(Task task);

    @Mapping(target = "sprintId", source = "sprint.sprintId")
    @Mapping(target = "account.accountId", source = "account.accountId")
    @Mapping(target = "account.email", source = "account.email")
    @Mapping(target = "account.fullName", source = "account.fullName")
    @Mapping(target = "syllabus.syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "syllabus.syllabusName", source = "syllabus.syllabusName")
    @Mapping(target = "subjectId", source = "subject.subjectId")
    @Mapping(target = "subjectStatus", source = "subject.status")
    @Mapping(target = "majorId", source = "major.majorId")
    TaskListResponse toTaskListResponse(Task task);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateTask(@MappingTarget Task task, TaskUpdateRequest request);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "major", ignore = true)
    Task toTask(TaskVPRequest request);

    @Mapping(target = "accountId", source = "account.accountId")
    @Mapping(target = "majorId", source = "major.majorId")
    TaskVPResponse toTaskVPResponse(Task task);

    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateTaskByVP(@MappingTarget Task task, TaskVPRequest request);
}
