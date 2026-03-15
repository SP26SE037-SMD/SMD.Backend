package com.example.smd.dto.request.plo;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PLOsCreateRequest {
    @NotBlank(message = "PLO_CODE_REQUIRED")
    String ploCode;
    String description;
}
