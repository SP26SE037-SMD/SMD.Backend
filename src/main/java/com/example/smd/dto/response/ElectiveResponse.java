package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElectiveResponse {
    UUID electiveId;
    String electiveCode;
    String electiveName;
    String description;
    Instant createdAt;
}
