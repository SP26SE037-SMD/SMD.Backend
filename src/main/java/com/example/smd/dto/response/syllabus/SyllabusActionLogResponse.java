package com.example.smd.dto.response.syllabus;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyllabusActionLogResponse {
    String logId;
    String syllabusId;
    String actionByEmail;
    String actionByFullName;
    String actionType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    java.time.Instant createdAt;
    String note;
}
