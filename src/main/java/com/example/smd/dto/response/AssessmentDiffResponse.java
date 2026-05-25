package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessmentDiffResponse {

    @Builder.Default
    List<String> addedAssessments = new ArrayList<>();

    @Builder.Default
    List<String> removedAssessments = new ArrayList<>();

    @Builder.Default
    List<AssessmentChangeDTO> changedAssessments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssessmentChangeDTO {
        private String assessmentIdentifier;
        private List<String> detailChanges = new ArrayList<>();
    }
}

