package com.example.smd.dto.response.clo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloAssessmentMappingResponse {

    String id;

    String cloId;
    String cloCode;
    String cloName;

    String assessmentId;
    Integer assessmentPart;
    String assessmentStatus;

    String syllabusId;
}
