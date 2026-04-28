package com.example.smd.dto.response.validate;

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
public class MappingCheckResponse {
    @JsonProperty("is_compliant")
    private boolean compliant;

    @JsonProperty("overall_coverage_score")
    private String overallCoverageScore;

    // Sửa 1: Đổi từ String sang List<String>
    @JsonProperty("uncovered_pos")
    private List<String> uncoveredPos;

    @JsonProperty("suggestions")
    private String suggestions;

    @JsonProperty("invalid_mappings")
    private List<MappingReport> invalidMappings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true) // Nên thêm ở class con cho an toàn
    public static class MappingReport {

        // Sửa 2: Khớp đúng chữ "plo_code"
        @JsonProperty("plo_code")
        private String ploCode;

        // Sửa 3: Khớp đúng chữ "po_code" và dùng kiểu String
        @JsonProperty("po_code")
        private String poCode;

        @JsonProperty("reason")
        private String reason;
    }
}
