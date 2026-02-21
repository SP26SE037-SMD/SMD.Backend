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
public class MajorRequest {
    @NotBlank(message = "MAJOR_CODE_REQUIRED")
    @Size(max = 20, message = "MAJOR_CODE_TOO_LONG")
    private String majorCode;

    @NotBlank(message = "MAJOR_NAME_REQUIRED")
    @Size(max = 100, message = "MAJOR_NAME_TOO_LONG")
    private String majorName;

    private String description;
}
