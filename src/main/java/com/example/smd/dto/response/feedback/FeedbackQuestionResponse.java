package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackQuestionResponse {
    String id;
    Integer questionNo;
    String questionText;
    String questionType;
    String formType;
    Boolean isRequired;
    Instant createdAt;
}
