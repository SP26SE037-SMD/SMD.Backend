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
public class PLOsRequest {
    @NotBlank(message = "PLO_CODE_REQUIRED")
    @Size(max = 20)
    String ploCode;
    @NotBlank(message = "PLO_NAME_REQUIRED")
    String ploName;
    String description;

    @NotBlank(message = "MAJOR_ID_REQUIRED")
    String majorId; // Khóa ngoại dùng để liên kết với Major
}
