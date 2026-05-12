package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewV2Response {

    UUID reviewId;
    String comment;
    LocalDate createdAt;

    /** Thông tin tóm tắt của task liên quan */
    TaskDto task;

    /** Người thực hiện review */
    ReviewerDto reviewer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDto {
        private UUID taskId;
        private String taskName;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerDto {
        private UUID accountId;
        private String fullName;
        private String email;
    }
}
