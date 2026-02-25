package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CLOsRequest {
    @NotBlank(message = "CLO_CODE_REQUIRED")
    String cloCode;

    @NotBlank(message = "CLO_NAME_REQUIRED")
    String cloName;

    String description;

    @NotNull(message = "BLOOM_LEVEL_REQUIRED")
    String bloomLevel;

    @NotBlank(message = "SUBJECT_ID_REQUIRED")
    String subjectId;
}
