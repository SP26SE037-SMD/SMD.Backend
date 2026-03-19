package com.example.smd.dto.request.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchTaskRequest {

    // sprintId has been moved to a PathVariable in the controller

    @NotEmpty(message = "TASK_LIST_REQUIRED")
    @Valid
    List<TaskItemRequest> tasks;
}
