package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectImportDTO {

    @ExcelColumn(name = "subjectCode", order = 0, required = true)
    String subjectCode;

    @ExcelColumn(name = "subjectName", order = 1, required = true)
    String subjectName;

    @ExcelColumn(name = "credits", order = 2, required = true)
    String credits;

    @ExcelColumn(name = "degreeLevel", order = 3)
    String degreeLevel;

    @ExcelColumn(name = "timeAllocation", order = 4)
    String timeAllocation;

    @ExcelColumn(name = "description", order = 5)
    String description;

    @ExcelColumn(name = "departmentCode", order = 6, required = true)
    String departmentCode;

    @ExcelColumn(name = "mintopass", order = 7)
    String minToPass;

    @ExcelColumn(name = "studentLimit", order = 8)
    String studentLimit;

    @ExcelColumn(name = "studentTasks", order = 9)
    String studentTasks;

    @ExcelColumn(name = "scoringScale", order = 10)
    String scoringScale;
}
