package com.example.smd.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportCurriculumResponse {
    UUID curriculumId;
    int total;
    int success;
    int failed;
    List<ImportCurriculumResult> details;
}
