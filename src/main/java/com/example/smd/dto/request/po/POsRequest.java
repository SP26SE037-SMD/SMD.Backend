package com.example.smd.dto.request.po;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class POsRequest {
    @NotBlank(message = "PO Code is required")
    private String poCode;
    private String description;
    @NotBlank(message = "Major ID is required")
    private String majorId;
}
