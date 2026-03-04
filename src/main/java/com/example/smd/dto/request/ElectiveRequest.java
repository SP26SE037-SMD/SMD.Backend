package com.example.smd.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElectiveRequest {
    @NotBlank(message = "ELECTIVE_CODE_REQUIRED")
    String electiveCode;

    @NotBlank(message = "ELECTIVE_NAME_REQUIRED")
    String electiveName;

    String description;

    @Min(value = 0, message = "MIN_CREDITS_INVALID")
    Integer minCreditsRequired;
}
