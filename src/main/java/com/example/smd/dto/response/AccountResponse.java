package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    String username;
    String email;
    String fullName;
    RoleResponse role; //String role
    Boolean isActive;
    Instant createdAt;
    Instant lastLogin;
}
