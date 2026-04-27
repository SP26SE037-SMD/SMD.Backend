package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProgramRegulationResponse {

    // --- THÔNG TIN NGÀNH HỌC (MỚI BỔ SUNG) ---
    @JsonProperty("major_code")
    private String majorCode;

    @JsonProperty("major_name")
    private String majorName;

    @JsonProperty("major_description")
    private String majorDescription;

    @JsonProperty("training_level")
    private String trainingLevel;

    // --- QUY ĐỊNH CHƯƠNG TRÌNH ---
    @JsonProperty("po_plo_rule")
    private String poPloRule;

    @JsonProperty("total_credits_rule")
    private String totalCreditsRule;

    @JsonProperty("excluded_credits_rule")
    private String excludedCreditsRule;

    @JsonProperty("general_education_credits")
    private String generalEducationCredits;

    @JsonProperty("professional_education_credits")
    private String professionalEducationCredits;

    // --- QUY CHẾ ĐÀO TẠO & SESSION ---
    @JsonProperty("theory_total_limit") // Bổ sung trường này vì Prompt đã có
    private String theoryTotalLimit;

    @JsonProperty("discussion_total_limit")
    private String discussionTotalLimit;

    @JsonProperty("max_periods_per_week")
    private String maxPeriodsPerWeek;

    @JsonProperty("self_study_auto_calc")
    private String selfStudyAutoCalc;

    @JsonProperty("assessment_rule")
    private String assessmentRule;

    // --- VALIDATION DATA ---
    @JsonProperty("course_catalog_validation")
    private String courseCatalogValidation;

    @JsonProperty("course_detail_mapping")
    private String courseDetailMapping;

    @JsonProperty("source_validation")
    private String sourceValidation;

}
