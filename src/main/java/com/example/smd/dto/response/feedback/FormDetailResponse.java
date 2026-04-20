package com.example.smd.dto.response.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormDetailResponse {
    String id;
    String googleFormId;
    String formUrl;
    String formEditUrl;
    Boolean isActive;
    Instant closeAt;
    List<SectionResponse> sections;
}
