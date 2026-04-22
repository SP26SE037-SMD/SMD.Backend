package com.example.smd.dto.response.request;

import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.account.AccountResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestResponse {
    UUID requestId;
    String title;
    String content;
    String comment;
    String status;

    UUID createdBy;
    CurriculumResponse curriculum;
    MajorResponse major;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant updatedAt;
}
