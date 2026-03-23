package com.example.smd.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionItemRequest {

    @NotNull(message = "SESSION_NUMBER_REQUIRED")
    @Min(value = 1, message = "SESSION_NUMBER_INVALID")
    Integer sessionNumber;

    @NotBlank(message = "SESSION_TITLE_REQUIRED")
    @Size(max = 200, message = "SESSION_TITLE_INVALID")
    String sessionTitle;

    String content;

    String teachingMethods;

    @Min(value = 0, message = "SESSION_DURATION_INVALID")
    Integer duration;
}
