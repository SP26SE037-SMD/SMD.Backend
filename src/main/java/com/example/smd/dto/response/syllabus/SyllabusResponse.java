package com.example.smd.dto.response.syllabus;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusResponse {
    String syllabusId;
    String syllabusName;
    Double minAvgGrade;
    String status;
    Instant createdAt;
    Instant approvedDate;

    // Trả thêm thông tin Subject để Frontend hiển thị tên môn học
    UUID subjectId;
    String subjectCode;
    String subjectName;
    Integer credit;
}
