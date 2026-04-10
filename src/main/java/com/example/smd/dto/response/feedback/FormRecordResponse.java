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
    String googleFormId;
    String formUrl;
    String formType;
    Boolean isActive;
    Instant createdAt;
}
