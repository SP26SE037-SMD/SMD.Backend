package com.example.smd.dto.excel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountExportDTO {

    @ExcelColumn(name = "Email", order = 0)
    private String email;

    @ExcelColumn(name = "Full Name", order = 1)
    private String fullName;

    @ExcelColumn(name = "Phone Number", order = 2)
    private String phoneNumber;

    @ExcelColumn(name = "Role", order = 3)
    private String role;
}
