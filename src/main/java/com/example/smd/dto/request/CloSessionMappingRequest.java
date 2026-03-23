package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CloSessionMappingRequest {

    @NotBlank(message = "CLO ID is required")
    String cloId;

    @NotBlank(message = "SESSION ID is required")
    String sessionId;
}
