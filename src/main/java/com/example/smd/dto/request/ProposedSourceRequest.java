package com.example.smd.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposedSourceRequest {

    @NotNull(message = "SOURCE_ID_REQUIRED")
    UUID sourceId;

    @NotNull(message = "SUBJECT_ID_REQUIRED")
    UUID subjectId;
}
