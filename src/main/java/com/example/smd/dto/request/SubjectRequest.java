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

    // Đổi sang String nếu bạn muốn nhập text mô tả như trong data mẫu test
    UUID preRequisite;

    String description;
    String studentTasks;
    Integer scoringScale;
    String decisionNo;
    Integer minToPass;

    // Phải là UUID để khớp với cột department_id trong DB của bạn
    UUID departmentId;
    UUID electiveId;
}
