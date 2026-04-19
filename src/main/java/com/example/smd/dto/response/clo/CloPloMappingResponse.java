package com.example.smd.dto.response.clo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloPloMappingResponse {
    String id;
    String cloId;
    String cloCode;
    String cloDescription;
    String ploId;
    String ploCode;
    String ploDescription;
    String contributionLevel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+7")
    Instant createdAt;
}
