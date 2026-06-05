package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormRecordResponse {
    String id;
    String curriculumId;
    String departmentId;
    String departmentName;
    String googleFormId;
    String formUrl;
    String formEditUrl;
    String formType;
    String description;
    Boolean isActive;
    Instant createdAt;
    Instant closeAt;
}
