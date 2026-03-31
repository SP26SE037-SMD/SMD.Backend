package com.example.smd.dto.request.clo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CLOsCreateRequest {
    @NotBlank(message = "CLO_CODE_REQUIRED")
    String cloCode;

    String cloName;

    String description;

    @NotNull(message = "BLOOM_LEVEL_REQUIRED")
    String bloomLevel;
}
