package com.example.smd.dto.response;

import com.example.smd.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {

    UUID notificationId;

    String title;

    String message;

    @Schema(example = "SYSTEM /  TASK_ASSIGNED / REVIEW_REQUEST / COMMENT / APPROVAL / REJECTION / REMINDER / DEADLINE / SPRINT_UPDATE")
    NotificationType type;

    Boolean isRead;

    UUID accountId;

    String accountEmail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;
}
