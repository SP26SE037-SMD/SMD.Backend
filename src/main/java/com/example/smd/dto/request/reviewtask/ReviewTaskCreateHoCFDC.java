package com.example.smd.dto.request.reviewtask;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewTaskCreateHoCFDC {
    String titleTask;
    String comment;
    String status;
    UUID taskId;
    UUID reviewerId;
    Boolean isAccepted;
}
