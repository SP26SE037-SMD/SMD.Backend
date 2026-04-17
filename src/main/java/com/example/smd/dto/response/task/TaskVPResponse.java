package com.example.smd.dto.response.task;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskVPResponse {
    UUID taskId;
    UUID accountId;
    UUID majorId;
    String taskName;
    String description;
    String status;
    String priority;
    String type;
    LocalDate deadline;
    LocalDate completedAt;
    LocalDate createdAt;
}
