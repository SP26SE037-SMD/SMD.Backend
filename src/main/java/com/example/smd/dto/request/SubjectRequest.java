package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectRequest {
    @NotBlank(message = "SUBJECT_CODE_REQUIRED")
    String subjectCode;

    @NotBlank(message = "SUBJECT_NAME_REQUIRED")
    String subjectName;

    Integer credits;
    String degreeLevel;
    String timeAllocation;
    String description;
    String studentTasks;
    Integer scoringScale;
    Integer minToPass;
    UUID departmentId;
    UUID electiveId;
}
