package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackQuestionRequest {
    Integer questionNo;
    String questionText;
    String questionType;
    String formType;
    Boolean isRequired;
}
