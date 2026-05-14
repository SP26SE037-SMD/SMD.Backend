package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentResponse {
    UUID assessmentId;
    UUID categoryId;
    String categoryName;
    UUID typeId;
    String typeName;
    UUID syllabusId;
    Integer part;
    Double weight;
    String completionCriteria;
    Integer duration;
    String questionType;
    String knowledgeSkill;
    String gradingGuide;
    String note;
    Instant createdAt;
}
