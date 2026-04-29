package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumGroupSubjectImportDTO {

    @ExcelColumn(name = "Group Code", order = 0, required = false)
    String groupCode;

    @ExcelColumn(name = "Subject Code", order = 1)
    String subjectCode;

    @ExcelColumn(name = "Subject Name", order = 2, required = false)
    String subjectName;

    @ExcelColumn(name = "Semester", order = 3, required = true)
    String semester;
}
