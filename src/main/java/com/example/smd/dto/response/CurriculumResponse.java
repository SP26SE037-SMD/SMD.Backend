package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurriculumResponse {

    UUID curriculumId;

    String curriculumCode;

    String curriculumName;

    Integer startYear;

    Integer endYear;

    String status;

    // Thông tin Major (nested object)
    MajorInfo major;

    // Inner class cho thông tin Major
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MajorInfo {
        String majorId;
        String majorCode;
        String majorName;
    }

}
