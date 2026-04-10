package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookSubmitRequest {
    String googleFormId;
    String submitterEmail;
    String submittedAt;
    List<AnswerPayload> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerPayload {
        String googleItemId;
        String questionTitle;
        String itemType;
        String answerValue;
    }
}
