package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkSemesterMappingResponse {

    boolean success;

    UUID curriculumId;

    // Summary của mapping
    Integer totalMappingsCreated;

    Integer totalSemestersMapped;

    // Breakdown by semester (e.g., {"3": 3, "5": 2, "6": 2})
    Map<Integer, Integer> mappingsBySemester;

    // Các lỗi validation nếu có
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<MappingError> errors;

    // Các cảnh báo (ví dụ: duplicate detected, đã tháy thế)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MappingError {
        String errorCode;

        String errorMessage;

        Integer semesterNo;

        UUID subjectId;

        UUID groupId;

        String details;
    }
}
