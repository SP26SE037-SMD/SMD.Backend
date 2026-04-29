package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurriculumImportDTO {

    @ExcelColumn(name = "Curriculum Code", order = 0, required = true)
    String curriculumCode;

    @ExcelColumn(name = "Name", order = 1, required = true)
    String curriculumName;

    @ExcelColumn(name = "Start Year", order = 2)
    String startYear;

    @ExcelColumn(name = "Description", order = 3)
    String description;

    @ExcelColumn(name = "Major Code", order = 4, required = true)
    String majorCode;

    @ExcelColumn(name = "PLO Code", order = 5, required = true)
    String ploCode;

    @ExcelColumn(name = "PLO Description", order = 6)
    String ploDescription;
}
