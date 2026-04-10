package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentRequest {
    @NotBlank(message = "DEPARTMENT_CODE_REQUIRED")
    String departmentCode;

    @NotBlank(message = "DEPARTMENT_NAME_REQUIRED")
    String departmentName;

    String description;
}