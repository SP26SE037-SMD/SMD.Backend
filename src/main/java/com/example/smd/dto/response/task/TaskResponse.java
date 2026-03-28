package com.example.smd.dto.response.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskResponse {
    UUID taskId;
    UUID sprintId;
    UUID accountId;
    UUID syllabusId;
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
