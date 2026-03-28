package com.example.smd.dto.response.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskListResponse {
    UUID taskId;
    UUID sprintId;
    TaskAccountResponse account;
    TaskSyllabusResponse syllabus;
    UUID subjectId;
    String taskName;
    String description;
    String status;
    String priority;
    String type;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate deadline;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdAt;
}
