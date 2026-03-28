package com.example.smd.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskCreateRequest {

    UUID subjectId;

    UUID sprintId;

    @NotBlank(message = "TASK_NAME_REQUIRED")
    @Size(max = 150, message = "TASK_NAME_TOO_LONG")
    String taskName;

    String description;

    @Size(max = 20, message = "PRIORITY_TOO_LONG")
    String priority;

    @Size(max = 50, message = "TASK_TYPE_TOO_LONG")
    String type;
}
