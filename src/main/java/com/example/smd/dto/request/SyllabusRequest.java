package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusRequest {
    UUID subjectId;
    String syllabusName;

    Double minAvgGrade;
}
