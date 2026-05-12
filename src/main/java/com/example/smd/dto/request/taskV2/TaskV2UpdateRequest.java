package com.example.smd.dto.request.taskV2;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskV2UpdateRequest {
     UUID assignTo;
     String taskName;
     String description;
    String action;
    Boolean isAccepted;
    String comment;
    String priority;
    String type;
    UUID targetId;
    UUID rootTaskId;
    LocalDate dueDate;
}
