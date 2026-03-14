package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoPloMappingRequest {
    @NotBlank(message = "PO ID is required")
    private String poId;
    @NotBlank(message = "PLO ID is required")
    private String ploId;
}
