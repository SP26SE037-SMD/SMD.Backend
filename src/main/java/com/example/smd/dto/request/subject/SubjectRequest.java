package com.example.smd.dto.request.subject;

import jakarta.persistence.Column;
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

    Integer minBloomLevel;
    Integer credits;
    String degreeLevel;
    String timeAllocation;
    String description;
    String studentTasks;
    Integer scoringScale;
    Integer minToPass;
    String tool;
    UUID departmentId;

    Integer theoryPeriods; // Số tiết lý thuyết (a)
    Integer practicalPeriods; // Số tiết thực hành/thảo luận (b)
    Integer selfStudyPeriods; // Số tiết tự học (c)
}
