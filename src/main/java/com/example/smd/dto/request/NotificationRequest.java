package com.example.smd.dto.request;

import com.example.smd.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationRequest {

    @NotBlank(message = "TITLE_REQUIRED")
    String title;
    
    @NotBlank(message = "MESSAGE_REQUIRED")
    String message;

    @NotNull(message = "TYPE_REQUIRED")
    NotificationType type;
    
    @NotNull(message = "ACCOUNT_ID_REQUIRED")
    UUID accountId;

    UUID taskId;

    UUID reviewId;
}
