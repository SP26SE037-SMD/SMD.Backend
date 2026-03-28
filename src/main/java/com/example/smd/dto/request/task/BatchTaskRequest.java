package com.example.smd.dto.request.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchTaskRequest {

    @NotEmpty(message = "TASK_LIST_REQUIRED")
    @Valid
    List<BatchTaskItemCreateRequest> tasks;
}
