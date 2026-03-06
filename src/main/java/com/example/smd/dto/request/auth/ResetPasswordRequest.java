package com.example.smd.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "TOKEN_REQUIRED")
    String accessToken;

    @NotBlank(message = "PASSWORD_REQUIRED")
    String newPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    String confirmPassword;
}
