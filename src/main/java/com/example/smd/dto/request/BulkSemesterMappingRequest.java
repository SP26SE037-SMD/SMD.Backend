package com.example.smd.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkSemesterMappingRequest {

    @NotNull(message = "CURRICULUM_ID_REQUIRED")
    UUID curriculumId;

    @Valid
    List<UUID> deleteSubjectsList;

    @NotEmpty(message = "SEMESTER_MAPPINGS_REQUIRED")
    @Valid
    List<SemesterMappingDTO> semesterMappings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SemesterMappingDTO {

        @NotNull(message = "SEMESTER_NO_REQUIRED")
        @Min(value = 1, message = "SEMESTER_NO_MIN_1")
        @Max(value = 10, message = "SEMESTER_NO_MAX_10")
        Integer semesterNo;

        @NotEmpty(message = "SUBJECTS_REQUIRED")
        @Valid
        List<SubjectGroupMappingDTO> subjects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SubjectGroupMappingDTO {

        @NotNull(message = "SUBJECT_ID_REQUIRED")
        UUID subjectId;

        // Nullable - can be null for required subjects (không group)
        UUID groupId;
    }
}
