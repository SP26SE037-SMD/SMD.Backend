package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackSubmissionRequest {
    UUID accountId;
    UUID curriculumId;
    List<FeedbackAnswerRequest> answers;
}
