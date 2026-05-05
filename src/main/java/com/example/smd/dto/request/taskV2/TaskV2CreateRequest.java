package com.example.smd.dto.request.taskV2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskV2CreateRequest {
    private UUID sprintId;
    private UUID assignTo;
    private String taskName;
    private String description;
    private String action;
    private String priority;
    private String type;
    private UUID targetId;
    private UUID rootTaskId;
    private LocalDate dueDate;
}
