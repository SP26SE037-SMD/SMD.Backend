package com.example.smd.dto.request.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionMaterialBlockUpdateRequest {

    @NotNull(message = "SESSION_ID_REQUIRED")
    UUID sessionId;

    @NotNull(message = "SESSION_NUMBER_REQUIRED")
    @Min(value = 1, message = "SESSION_NUMBER_INVALID")
    Integer sessionNumber;

    @NotBlank(message = "SESSION_TITLE_REQUIRED")
    @Size(max = 200, message = "SESSION_TITLE_INVALID")
    String sessionTitle;

    String teachingMethods;

    @Min(value = 0, message = "SESSION_DURATION_INVALID")
    Integer duration;

    @NotEmpty(message = "SESSION_LIST_REQUIRED")
    List<UUID> material;

    @NotEmpty(message = "BLOCK_LIST_EMPTY")
    List<UUID> block;
}
