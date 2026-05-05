package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskV2Response {
    private UUID taskId;
    private UUID sprintId;
    private AccountDto assignTo;
    private AccountDto createdBy;
    private String taskName;
    private String description;
    private String status;
    private String action;
    private String priority;
    private String type;
    private LocalDate dueDate;
    private LocalDate completedAt;
    private LocalDate createdAt;

    private SyllabusDto syllabus;
    private CurriculumDto curriculum;
    private DocumentDto document;

    @Data
    @Builder
    public static class AccountDto {
        private UUID accountId;
        private String email;
        private String fullName;
    }

    @Data
    @Builder
    public static class SyllabusDto {
        private UUID syllabusId;
        private String syllabusName;
    }

    @Data
    @Builder
    public static class CurriculumDto {
        private UUID curriculumId;
        private String curriculumCode;
        private String curriculumName;
    }

    @Data
    @Builder
    public static class DocumentDto {
        private UUID documentId;
        private String documentUrl;
    }
}
