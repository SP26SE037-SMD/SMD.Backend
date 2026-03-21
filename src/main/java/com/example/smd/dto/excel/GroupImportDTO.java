package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupImportDTO {

    @ExcelColumn(name = "Group Code", order = 0, required = true)
    String groupCode;

    @ExcelColumn(name = "Group Name", order = 1)
    String groupName;

    @ExcelColumn(name = "description", order = 2)
    String description;

    @ExcelColumn(name = "type", order = 3)
    String type;
}
