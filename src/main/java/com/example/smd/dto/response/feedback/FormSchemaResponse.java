package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormSchemaResponse {
    String formId;
    String title;
    String description;
    List<SectionSchema> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SectionSchema {
        String sectionId;
        String title;
        String actionAfter;
        String targetSectionId;
        List<QuestionSchema> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuestionSchema {
        String questionId;
        String type;
        String content;
        Boolean isRequired;
        List<OptionSchema> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OptionSchema {
        String optionId;
        String text;
        String goToSectionId;
    }
}
