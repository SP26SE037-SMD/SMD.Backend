package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormSubmissionAnswerResponse {
    String id;
    String questionId;
    String questionText;
    String selectedOptionId;
    String selectedOptionText;
    String answerText;
}
