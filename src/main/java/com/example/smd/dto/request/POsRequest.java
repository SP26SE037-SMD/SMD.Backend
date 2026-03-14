package com.example.smd.dto.request;

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
    @NotBlank(message = "PO Name is required") // Thêm dòng này
    private String poName;
    private String description;
    @NotBlank(message = "Major ID is required")
    private String majorId;
}
