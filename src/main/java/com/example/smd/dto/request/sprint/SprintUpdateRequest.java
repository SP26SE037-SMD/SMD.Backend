package com.example.smd.dto.request.sprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SprintUpdateRequest {
    @NotBlank(message = "SPRINT_NAME_REQUIRED")
    @Size(max = 100, message = "SPRINT_NAME_TOO_LONG")
    String sprintName;

    Instant startDate;

    Instant endDate;

    @Size(max = 20, message = "STATUS_TOO_LONG")
    String status;
}
