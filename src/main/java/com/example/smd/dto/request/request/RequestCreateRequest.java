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
public class RequestCreateRequest {

    @NotBlank(message = "REQUEST_TITLE_REQUIRED")
    @Size(max = 50, message = "REQUEST_TITLE_TOO_LONG")
    String title;

    String content;

    /** SYLLABUS | CURRICULUM | MAJOR | SUBJECT | TASK */
    @Size(max = 50, message = "REQUEST_TYPE_TOO_LONG")
    String type;

    /** ID của đối tượng liên quan (Syllabus, Curriculum, ...) */
    UUID targetId;

    UUID receivedById;
}
