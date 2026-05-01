package com.example.smd.dto.response.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentCloMappingValidationResult {

    @JsonProperty("is_valid")
    private boolean logicValid;

    // 1. Cờ trạng thái kiểm định
    @JsonProperty("is_all_clos_mapped")
    private boolean isAllClosMapped;

    @JsonProperty("is_all_assessments_mapped")
    private boolean isAllAssessmentsMapped;

    // 2. Dữ liệu CLO chưa được map (Bị sót)
    @JsonProperty("unmapped_clos")
    private List<UnmappedCloSuggestionResponse> unmappedClos;

    // 3. Dữ liệu Assessment chưa được map (Dư thừa/Chưa rõ mục đích)
    @JsonProperty("unmapped_assessments")
    private List<UnmappedAssessmentSuggestionResponse> unmappedAssessments;

    // 4. Danh sách mapping thành công trả về
    @JsonProperty("data")
    private List<AssessmentCloMappingItemResponse> data;

    // ==========================================
    // INNER CLASSES TƯƠNG ỨNG VỚI CẤU TRÚC JSON
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnmappedCloSuggestionResponse {

        @JsonProperty("clo_id")
        private UUID cloId;

        @JsonProperty("clo_code")
        private String cloCode;

        @JsonProperty("suggestion")
        private String suggestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnmappedAssessmentSuggestionResponse {

        @JsonProperty("assessment_id")
        private UUID assessmentId;

        @JsonProperty("question_type")
        private String questionType;

        @JsonProperty("suggestion")
        private String suggestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssessmentCloMappingItemResponse {

        @JsonProperty("assessment_id")
        private UUID assessmentId;

        @JsonProperty("clo_id")
        private UUID cloId;

        @JsonProperty("confidence_score")
        private Double confidenceScore;

        @JsonProperty("reasoning")
        private String reasoning;
    }
}
