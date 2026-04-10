package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormSubmissionResponse {
    String id;
    String accountId;
    String curriculumId;
    Instant submittedAt;
    List<FormSubmissionAnswerResponse> answers;
}
