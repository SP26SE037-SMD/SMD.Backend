package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
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
    String preRequisite;
    String description;
    String studentTasks;
    Integer scoringScale;
    String decisionNo;

    Boolean isApproved;
    Instant approvedDate; // Dùng Instant để đồng bộ với Entity

    // Map từ field 'status' trong Entity của bạn
    Boolean status;

    // Trả về UUID của khoa để tiện cho việc truy vấn phía Frontend
    UUID departmentId;

    UUID electiveId;
    Instant createdAt;
}
