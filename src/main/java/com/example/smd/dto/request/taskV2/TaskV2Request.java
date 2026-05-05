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
public class TaskV2Request {
    private UUID sprintId;
    private UUID assignTo;
    private UUID createdBy;
    private String taskName;
    private String description;
    private String status;
    private String action;
    private String priority;
    private String type;
    private UUID targetId;
    private UUID rootTaskId;
    private LocalDate dueDate;
    private LocalDate completedAt;
}
