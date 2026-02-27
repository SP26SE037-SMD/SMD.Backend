package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CLOsResponse {
    UUID cloId;
    String cloCode;
    String cloName;
    String description;
    String bloomLevel;
    String subjectId;
    String subjectName;
}
