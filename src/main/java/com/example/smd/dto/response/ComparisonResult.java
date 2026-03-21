package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComparisonResult {
    @JsonProperty("removed_concepts")
    private List<String> removedConcepts;

    @JsonProperty("added_concepts")
    private List<String> addedConcepts;

    @JsonProperty("modified_concepts") // Optional
    private List<String> modifiedConcepts;

    @JsonProperty("risk_assessment")
    private String riskAssessment; // "HIGH", "MEDIUM", "LOW"

    @JsonProperty("risk_reason")
    private String riskReason;
}
