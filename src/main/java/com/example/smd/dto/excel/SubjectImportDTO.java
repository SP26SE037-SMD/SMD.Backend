package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubjectImportDTO {

    @ExcelColumn(name = "Subject Code", order = 0, required = true)
    String subjectCode;

    @ExcelColumn(name = "Subject Name", order = 1, required = true)
    String subjectName;

    @ExcelColumn(name = "Description", order = 2)
    String description;

    @ExcelColumn(name = "Department Code", order = 3, required = true)
    String departmentCode;

    @ExcelColumn(name = "Credits", order = 4, required = true)
    String credits;

    @ExcelColumn(name = "Degree Level", order = 5)
    String degreeLevel;

    @ExcelColumn(name = "Time Allocation", order = 6)
    String timeAllocation;

    @ExcelColumn(name = "Min To Pass", order = 7)
    String minToPass;

    @ExcelColumn(name = "Student Limit", order = 8)
    String studentLimit;

    @ExcelColumn(name = "Student Tasks", order = 9)
    String studentTasks;

    @ExcelColumn(name = "Scoring Scale", order = 10)
    String scoringScale;

    @ExcelColumn(name = "Min Bloom Level", order = 11)
    String minBloomLevel;

    @ExcelColumn(name = "Theory Period", order = 12)
    String theoryPeriods;

    @ExcelColumn(name = "Practical Period", order = 13)
    String practicalPeriods;

    @ExcelColumn(name = "Self Study Period", order = 14)
    String selfStudyPeriods;
}
