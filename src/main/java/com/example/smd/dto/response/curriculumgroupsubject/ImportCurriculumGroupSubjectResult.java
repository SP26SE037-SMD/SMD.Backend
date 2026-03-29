package com.example.smd.dto.response.curriculumgroupsubject;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportCurriculumGroupSubjectResult {
    Integer rowNumber;
    String groupCode;
    String subjectCode;
    String semester;
    String status;
    String message;
}
