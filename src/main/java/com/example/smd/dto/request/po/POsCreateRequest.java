package com.example.smd.dto.request.po;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class POsCreateRequest {
    @NotBlank(message = "PO Code is required")
    private String poCode;
    private String description;
}
