package com.example.smd.dto.response.prerequisite;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportPrerequisiteResponse {
    int total;
    int success;
    int failed;
    List<ImportPrerequisiteResult> details;
}
