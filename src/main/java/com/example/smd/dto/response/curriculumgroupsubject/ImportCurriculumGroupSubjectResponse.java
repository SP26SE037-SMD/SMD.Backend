package com.example.smd.dto.response.curriculumgroupsubject;

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
public class ImportCurriculumGroupSubjectResponse {
    int total;
    int success;
    int failed;
    List<ImportCurriculumGroupSubjectResult> details;
}
