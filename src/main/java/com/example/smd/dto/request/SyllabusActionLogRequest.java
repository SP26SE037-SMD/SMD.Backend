package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusActionLogRequest {
    UUID syllabusId;
    String actionByEmail; // Hoặc ID tùy theo logic Security của bạn
    String actionType;
    String note;
}
