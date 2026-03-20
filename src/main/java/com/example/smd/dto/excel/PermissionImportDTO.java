package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionImportDTO {

    @ExcelColumn(name = "Permission Name", order = 0, required = true)
    String permissionName;

    @ExcelColumn(name = "Description", order = 1)
    String description;
}
