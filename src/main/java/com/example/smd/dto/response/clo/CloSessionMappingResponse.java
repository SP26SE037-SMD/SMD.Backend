package com.example.smd.dto.response.clo;

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
public class CloSessionMappingResponse {

    String id;

    String cloId;
    String cloCode;
    String cloName;

    String sessionId;
    Integer sessionNumber;
    String sessionTitle;
    String sessionStatus;

    String syllabusId;
}
