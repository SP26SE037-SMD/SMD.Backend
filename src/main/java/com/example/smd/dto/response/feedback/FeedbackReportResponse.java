package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackReportResponse {
    String formId;
    Integer totalSubmissions;
    List<QuestionReport> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuestionReport {
        String questionId;
        String questionText;
        String type;
        Map<String, Integer> optionCounts;
        List<String> textAnswers;
        Double averageRating;
    }
}
