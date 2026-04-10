package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectResponse {
    UUID subjectId;
    String subjectCode;
    String subjectName;
    Integer credits;
    String degreeLevel;
    String timeAllocation;
    String description;
    String studentTasks;
    Integer scoringScale;
    String decisionNo;
    String tool;
    Integer minToPass;
    Integer minBloomLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant approvedDate; // Dùng Instant để đồng bộ với Entity

    // Map từ field 'status' trong Entity của bạn
    String status;

    // Trả về UUID của khoa để tiện cho việc truy vấn phía Frontend
    DepartmentResponse department;
    List<PrerequisiteResponse> preRequisite;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;
}
