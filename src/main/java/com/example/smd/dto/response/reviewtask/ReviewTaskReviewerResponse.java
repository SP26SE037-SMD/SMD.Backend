package com.example.smd.dto.response.reviewtask;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewTaskReviewerResponse {
    UUID reviewerId;
    String fullName;
    String email;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    String avatarUrl;
    String role;
}
