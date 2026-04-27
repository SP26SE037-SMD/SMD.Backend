package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Thêm để hỗ trợ tạo object nhanh
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComplianceCheckResponse {

    @JsonProperty("overall_similarity_score")
    private String overallSimilarityScore;

    @JsonProperty("is_compliant")
    private boolean compliant;

    @JsonProperty("has_internal_duplicates")
    private boolean hasInternalDuplicates;

    @JsonProperty("mismatch_details")
    private String mismatchDetails;

    @JsonProperty("suggestions")
    private String suggestions;

    @JsonProperty("mapping_report")
    private List<MappingReport> mappingReport;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MappingReport {
        @JsonProperty("code")
        private String code;

        @JsonProperty("status")
        private String status;
    }
}

