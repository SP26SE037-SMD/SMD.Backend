package com.example.smd.dto.response.account;

import com.example.smd.dto.response.RoleResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String phoneNumber;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String avatarUrl;
    @Schema(example = "ADMIN / LECTURER / STUDENT / COLLABORATOR / HoCFDC /  HoPDC / PDCM /  ")
    RoleResponse role;
    Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant lastLogin;
}
