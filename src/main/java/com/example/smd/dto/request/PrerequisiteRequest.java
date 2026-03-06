package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrerequisiteRequest {
    @NotBlank(message = "SUBJECT_ID_REQUIRED")
    String subjectId;

    @NotBlank(message = "PREREQUISITE_ID_REQUIRED")
    String prerequisiteSubjectId;

    Boolean isMandatory;
}


