package com.example.smd.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskRequest {

    @NotNull(message = "SPRINT_ID_REQUIRED")
    UUID sprintId;

    UUID accountId;

    UUID syllabusId;

    @NotBlank(message = "TASK_NAME_REQUIRED")
    @Size(max = 150, message = "TASK_NAME_TOO_LONG")
    String taskName;

    String description;

    @Size(max = 20, message = "STATUS_TOO_LONG")
    String status;

    @Size(max = 20, message = "PRIORITY_TOO_LONG")
    String priority;

    Instant deadline;
}
