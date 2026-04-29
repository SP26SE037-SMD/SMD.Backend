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
public class CloPloMappingCheckResponse {
    @JsonProperty("is_logic_valid")
    private boolean logicValid;

    @JsonProperty("suggestions")
    private String suggestions;

    @JsonProperty("invalid_mappings")
    private List<MappingReport> invalidMappings;

    @JsonProperty("wrong_level_warnings")
    private List<WarningReport> wrongLevelWarnings;

    @JsonProperty("unmapped_clos")
    private List<String> unmapped_clos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true) // Nên thêm ở class con cho an toàn
    public static class MappingReport {

        // Sửa 2: Khớp đúng chữ "clo_code"
        @JsonProperty("clo_code")
        private String cloCode;

        // Sửa 3: Khớp đúng chữ "plo_code" và dùng kiểu String
        @JsonProperty("plo_code")
        private String ploCode;

        @JsonProperty("reason")
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true) // Nên thêm ở class con cho an toàn
    public static class WarningReport {

        // Sửa 2: Khớp đúng chữ "clo_code"
        @JsonProperty("clo_code")
        private String cloCode;

        // Sửa 3: Khớp đúng chữ "plo_code" và dùng kiểu String
        @JsonProperty("plo_code")
        private String ploCode;

        @JsonProperty("warning")
        private String warning;
    }
}
