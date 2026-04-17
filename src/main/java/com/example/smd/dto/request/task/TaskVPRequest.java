package com.example.smd.dto.request.task;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskVPRequest {
    UUID taskId;
    UUID accountId;
    UUID majorId;
    String taskName;
    String description;
    String priority;
    LocalDate deadline;
    String type;
}
