package com.example.smd.dto.request.reviewtask;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewTaskCreateRequest {
    String titleTask;
    Instant dueDate;
    String status;
    UUID taskId;
    UUID reviewerId;
}
