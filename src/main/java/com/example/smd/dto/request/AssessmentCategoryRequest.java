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
public class AssessmentCategoryRequest {

    @NotBlank(message = "ASSESSMENT_CATEGORY_NAME_REQUIRED")
    @Size(max = 50, message = "ASSESSMENT_CATEGORY_NAME_INVALID")
    String categoryName;

    String description;
}
