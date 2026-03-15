package com.example.smd.dto.request.plo;

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
    String description;

    @NotBlank(message = "CURRICULUM_ID_REQUIRED")
    String curriculumId; // Khóa ngoại dùng để liên kết với Curriculum
}
