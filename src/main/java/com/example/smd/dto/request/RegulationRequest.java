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

import java.util.UUID;

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
    @NotNull(message = "REGULATION_VALUE_REQUIRED")
    String value;

    @NotNull(message = "MAJOR_ID_REQUIRED")
    UUID majorId;
}
