package com.example.smd.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountProfileResponse {

    UUID accountId;

    String avatarUrl;

    String phoneNumber;
}

