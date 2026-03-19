package com.example.smd.dto.request.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    String email;

    @Size(min = 6, message = "PASSWORD_INVALID")
    String password;

    @Size(max = 100, message = "FULL_NAME_INVALID")
    String fullName;

    String roleName;

    Boolean isActive;

    String departmentCode;
}
