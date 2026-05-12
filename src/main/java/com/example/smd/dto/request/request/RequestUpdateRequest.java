package com.example.smd.dto.request.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestUpdateRequest {

    String comment;

    @Size(max = 50, message = "STATUS_TOO_LONG")
    String status;

    UUID receivedById;
}
