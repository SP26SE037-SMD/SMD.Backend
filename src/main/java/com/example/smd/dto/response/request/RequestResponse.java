package com.example.smd.dto.response.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class RequestResponse {

    UUID requestId;
    String title;
    String content;
    String comment;
    String status;
    String type;
    UUID targetId;

    AccountDto createdBy;
    AccountDto receivedBy;

    // Enriched target detail — only one will be non-null depending on 'type'
    SubjectDto    subject;
    SyllabusDto   syllabus;
    CurriculumDto curriculum;
    MajorDto      major;
    TaskDto       task;
    SprintDto     sprint;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDto {
        UUID accountId;
        String email;
        String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDto {
        UUID   subjectId;
        String subjectCode;
        String subjectName;
        Integer credits;
        String status;
        String departmentCode;
        String departmentName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyllabusDto {
        UUID   syllabusId;
        String syllabusName;
        String status;
        // parent subject info
        UUID   subjectId;
        String subjectCode;
        String subjectName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurriculumDto {
        UUID   curriculumId;
        String curriculumCode;
        String curriculumName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MajorDto {
        UUID   majorId;
        String majorCode;
        String majorName;
        String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDto {
        UUID   taskId;
        String taskName;
        String status;
        String type;
        String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintDto {
        UUID   sprintId;
        String sprintName;
    }
}
