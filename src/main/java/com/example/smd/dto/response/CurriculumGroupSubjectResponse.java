package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurriculumGroupSubjectResponse {
    UUID id;
    UUID curriculumId;
    String curriculumCode;
    String curriculumName;
    UUID groupId;
    String groupCode;
    String groupName;
    UUID subjectId;
    String subjectCode;
    String subjectName;
    Integer semester;
}
