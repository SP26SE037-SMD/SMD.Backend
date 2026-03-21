package com.example.smd.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssessmentRequest {

    @NotBlank(message = "ASSESSMENT_CATEGORY_ID_REQUIRED")
    String categoryId;

    @NotBlank(message = "ASSESSMENT_TYPE_ID_REQUIRED")
    String typeId;

    @NotBlank(message = "SYLLABUS_ID_REQUIRED")
    String syllabusId;

    @NotNull(message = "ASSESSMENT_PART_REQUIRED")
    @Min(value = 1, message = "ASSESSMENT_PART_INVALID")
    Integer part;

    @NotNull(message = "ASSESSMENT_WEIGHT_REQUIRED")
    @DecimalMin(value = "0.01", message = "ASSESSMENT_WEIGHT_INVALID")
    @DecimalMax(value = "100.0", message = "ASSESSMENT_WEIGHT_INVALID")
    Double weight;

    String completionCriteria;

    @Min(value = 0, message = "ASSESSMENT_DURATION_INVALID")
    Integer duration;

    @Size(max = 50, message = "ASSESSMENT_QUESTION_TYPE_INVALID")
    String questionType;

    @Size(max = 50, message = "ASSESSMENT_KNOWLEDGE_SKILL_INVALID")
    String knowledgeSkill;

    String gradingGuide;

    String note;

    @Size(max = 20, message = "ASSESSMENT_STATUS_INVALID")
    String status;
}
