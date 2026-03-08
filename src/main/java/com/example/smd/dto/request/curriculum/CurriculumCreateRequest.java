package com.example.smd.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumCreateRequest {

    @NotBlank(message = "CURRICULUM_CODE_REQUIRED")
    @Size(max = 20, message = "CURRICULUM_CODE_TOO_LONG")
    String curriculumCode;

    @NotBlank(message = "CURRICULUM_NAME_REQUIRED")
    @Size(max = 100, message = "CURRICULUM_NAME_TOO_LONG")
    String curriculumName;

    Integer startYear;
}
