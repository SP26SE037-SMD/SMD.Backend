package com.example.smd.dto.excel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountImportDTO {

    @ExcelColumn(name = "Email", order = 0, required = true)
    private String email;

    @ExcelColumn(name = "Full Name", order = 1, required = true)
    private String fullName;

    @ExcelColumn(name = "Phone Number", order = 2, required = true)
    private String phoneNumber;

    @ExcelColumn(name = "Department Code", order = 3, required = true)
    private String departmentCode;

}
