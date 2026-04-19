package com.example.smd.dto.response.reviewtask;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ReviewTaskResponse {
    UUID reviewId;
    String titleTask;
    String commentMaterial;
    String commentSession;
    String commentAssessment;
    Boolean isAccepted;
    String comment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant reviewDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant dueDate;

    String status;
    ReviewTaskTaskResponse task;
    ReviewTaskReviewerResponse reviewer;
}
