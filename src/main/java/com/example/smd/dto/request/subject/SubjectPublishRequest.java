package com.example.smd.dto.request.subject;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectPublishRequest {
    @NotBlank(message = "DECISION_NO_REQUIRED")
    String decisionNo;
}