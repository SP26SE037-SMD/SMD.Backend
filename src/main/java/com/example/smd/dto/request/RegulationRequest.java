package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegulationRequest {

    @NotBlank(message = "REGULATION_CODE_REQUIRED")
    @Size(max = 50, message = "REGULATION_CODE_INVALID")
    String code;

    @NotBlank(message = "REGULATION_NAME_REQUIRED")
    @Size(max = 100, message = "REGULATION_NAME_INVALID")
    String name;

    String description;

    @NotNull(message = "REGULATION_VALUE_REQUIRED")
    Integer value;
}
