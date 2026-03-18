package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrerequisiteImportDTO {

    @ExcelColumn(name = "Subject code", order = 0, required = true)
    String subjectCode;

    @ExcelColumn(name = "Subject Prerequisite", order = 1, required = true)
    String prerequisiteSubjectCode;

    @ExcelColumn(name = "isMandantory", order = 2)
    String isMandatory;
}
