package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentResponse {
    String assessmentId;
    String categoryId;
    String categoryName;
    String typeId;
    String typeName;
    String syllabusId;
    Integer part;
    Double weight;
    String completionCriteria;
    Integer duration;
    String questionType;
    String knowledgeSkill;
    String gradingGuide;
    String note;
    String status;
    Instant createdAt;
}
