package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssessmentTypeRequest {

    @NotBlank(message = "ASSESSMENT_TYPE_NAME_REQUIRED")
    @Size(max = 50, message = "ASSESSMENT_TYPE_NAME_INVALID")
    String typeName;
}
