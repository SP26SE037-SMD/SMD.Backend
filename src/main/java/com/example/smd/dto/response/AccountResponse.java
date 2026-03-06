package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {
    java.util.UUID accountId;
    String email;
    String fullName;
    @Schema(example = "ADMIN / LECTURER / STUDENT / COLLABORATOR / HoCFDC /  HoPDC / PDCM /  ")
    String role;
    Boolean isActive;
    Instant createdAt;
    Instant lastLogin;
}
