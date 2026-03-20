package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaterialRequest {
    @NotBlank(message = "Material title is required")
    String title;
    String materialType;

    @NotNull(message = "Syllabus ID is required")
    UUID syllabusId;
}
