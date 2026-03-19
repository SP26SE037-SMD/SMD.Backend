package com.example.smd.dto.excel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
//@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountExportDTO {

    @ExcelColumn(name = "Email", order = 0)
    private String email;

    @ExcelColumn(name = "Full Name", order = 1)
    private String fullName;

    @ExcelColumn(name = "Phone Number", order = 2)
    private String phoneNumber;

    @ExcelColumn(name = "Department Code", order = 3)
    private String departmentCode;

    @ExcelColumn(name = "Department Name", order = 4)
    private String departmentName;

    @ExcelColumn(name = "Role", order = 5)
    private String role;

    public AccountExportDTO(
            String email,
            String fullName,
            String phoneNumber,
            String departmentCode,
            String departmentName,
            String role
    ) {
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.role = role;
    }
}
