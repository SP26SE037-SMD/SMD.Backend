package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SprintCurriculumResponse {

    UUID sprintId;
    String sprintName;
    UUID curriculumId;
    String curriculumCode;
}
