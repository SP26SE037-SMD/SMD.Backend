package com.example.smd.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskRequest {


    @NotNull(message = "ACCOUNT_ID_REQUIRED")
    UUID accountId;

    UUID syllabusId;

    UUID curriculumId;

    @NotBlank(message = "TASK_NAME_REQUIRED")
    @Size(max = 150, message = "TASK_NAME_TOO_LONG")
    String taskName;

    String description;

    @Size(max = 20, message = "STATUS_TOO_LONG")
    String status;

    @Size(max = 20, message = "PRIORITY_TOO_LONG")
    String priority;

    UUID sprintId;

    LocalDate deadline;

    @Size(max = 50, message = "TASK_TYPE_TOO_LONG")
    String type;

    LocalDate createdAt;
}
