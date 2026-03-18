package com.example.smd.dto.response.prerequisite;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportPrerequisiteResult {
    String subjectCode;
    String prerequisiteSubjectCode;
    String status;
    String message;
}
