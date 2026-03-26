package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackFormResponse {
    String formType;
    List<FeedbackQuestionResponse> questions;
    List<FeedbackOptionResponse> options;
}
