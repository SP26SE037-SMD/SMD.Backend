package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleFormCreatedRequest {
    String googleFormId;
    String formUrl;
    String editUrl;
    Instant closeAt;
    List<QuestionMappingItem> questionMapping;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuestionMappingItem {
        String questionId;
        String googleItemId;
        String sectionId;
    }
}
