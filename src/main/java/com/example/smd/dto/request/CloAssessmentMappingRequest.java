package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CloAssessmentMappingRequest {

    @NotBlank(message = "CLO ID is required")
    String cloId;

    @NotBlank(message = "Assessment ID is required")
    String assessmentId;
}
