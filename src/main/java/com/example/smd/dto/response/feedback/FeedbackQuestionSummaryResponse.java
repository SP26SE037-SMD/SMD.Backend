package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackQuestionSummaryResponse {
    String questionId;
    String questionText;
    String questionType;
    long totalAnswers;
    List<FeedbackOptionStatResponse> optionStats;
    long textAnswerCount;
}
