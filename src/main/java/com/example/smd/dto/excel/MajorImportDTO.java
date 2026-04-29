package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MajorImportDTO {

    @ExcelColumn(name = "Major Code", order = 0, required = true)
    String majorCode;

    @ExcelColumn(name = "Name", order = 1, required = true)
    String majorName;

    @ExcelColumn(name = "Description", order = 2)
    String description;

    @ExcelColumn(name = "PO Code", order = 3, required = true)
    String poCode;

    @ExcelColumn(name = "PO Description", order = 4)
    String poDescription;
}
