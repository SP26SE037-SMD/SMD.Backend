package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboImportDTO {

    @ExcelColumn(name = "Combo Code", order = 0, required = true)
    String comboCode;

    @ExcelColumn(name = "Combo Name", order = 1)
    String comboName;

    @ExcelColumn(name = "description", order = 2)
    String description;

    @ExcelColumn(name = "type", order = 3)
    String type;
}
