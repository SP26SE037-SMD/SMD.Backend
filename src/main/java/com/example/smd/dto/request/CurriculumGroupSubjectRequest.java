package com.example.smd.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumGroupSubjectRequest {

    @NotNull(message = "CURRICULUM_ID_REQUIRED")
    UUID curriculumId;

    UUID groupId; // Optional - có thể null

    @NotNull(message = "SUBJECT_ID_REQUIRED")
    UUID subjectId;

    Integer semester; // Học kỳ khuyến nghị
}
