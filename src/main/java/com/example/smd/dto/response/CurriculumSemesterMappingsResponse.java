package com.example.smd.dto.response;

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
public class CurriculumSemesterMappingsResponse {

    UUID curriculumId;

    List<SemesterMappingItem> semesterMappings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SemesterMappingItem {
        Integer semesterNo;
        List<SubjectMappingItem> subjects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SubjectMappingItem {
        UUID subjectId;
        String subjectCode;
        String subjectName;
        UUID comboId;
    }
}
