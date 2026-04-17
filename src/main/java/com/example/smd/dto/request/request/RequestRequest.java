package com.example.smd.dto.request.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestRequest {
    @NotBlank(message = "REQUEST_TITLE_REQUIRED")
    @Size(max = 50, message = "REQUEST_TITLE_TOO_LONG")
    String title;

    String content;

    String comment;

    @Size(max = 50, message = "STATUS_TOO_LONG")
    String status;

    UUID createdById;

    UUID curriculumId;

    UUID majorId;
}
