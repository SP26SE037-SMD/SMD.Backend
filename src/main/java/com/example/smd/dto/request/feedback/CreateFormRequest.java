package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFormRequest {
    UUID curriculumId;
    String formType;
    Instant closeAt;
}
