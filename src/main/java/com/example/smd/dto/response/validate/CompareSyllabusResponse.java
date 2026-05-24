package com.example.smd.dto.response.validate;

import com.example.smd.dto.response.AssessmentDiffResponse;
import com.example.smd.dto.response.ComparisonResult;
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
public class CompareSyllabusResponse {
    UUID oldSyllabusId;
    UUID newSyllabusId;
    AssessmentDiffResponse assessmentDiffResponse;
    ComparisonResult comparisonResult;
}
