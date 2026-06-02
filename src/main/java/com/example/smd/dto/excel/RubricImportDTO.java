package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO phẳng (Flat-row) ánh xạ 1-1 với từng dòng trong file Excel.
 *
 * Cấu trúc cột Excel (theo thứ tự):
 * | Rubric Code | Rubric Name | Criteria Code | Criteria Name | Weight | Level | Description |
 * |      0      |      1      |       2       |       3       |    4   |   5   |      6      |
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RubricImportDTO {

    @ExcelColumn(name = "Rubric Code", order = 0, required = true)
    String rubricCode;

    @ExcelColumn(name = "Rubric Name", order = 1, required = true)
    String rubricName;

    @ExcelColumn(name = "Criteria Code", order = 2, required = true)
    String criteriaCode;

    @ExcelColumn(name = "Criteria Name", order = 3, required = true)
    String criteriaName;

    @ExcelColumn(name = "Weight", order = 4, required = true)
    String weight;

    @ExcelColumn(name = "Level", order = 5, required = true)
    String level;

    @ExcelColumn(name = "Description", order = 6, required = false)
    String description;
}
