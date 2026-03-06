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
public class AccountUpdateRequest {

    @Size(min = 6, message = "PASSWORD_INVALID")
    String password;

    @Size(max = 100, message = "FULL_NAME_INVALID")
    String fullName;
}
