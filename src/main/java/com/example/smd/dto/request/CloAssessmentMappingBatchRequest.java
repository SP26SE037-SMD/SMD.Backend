package com.example.smd.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloAssessmentMappingBatchRequest {

    @NotEmpty(message = "MAPPING_LIST_REQUIRED")
    @Valid
    List<CloAssessmentMappingRequest> mappings;
}
