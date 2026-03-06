package com.example.smd.dto.request;

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
public class SystemLogRequest {

    UUID accountId; // Nếu null, sẽ lấy từ user đang đăng nhập

    @NotBlank(message = "ACTION_REQUIRED")
    @Size(max = 100, message = "ACTION_TOO_LONG")
    String action;

    UUID targetId; // ID của đối tượng bị tác động
}
