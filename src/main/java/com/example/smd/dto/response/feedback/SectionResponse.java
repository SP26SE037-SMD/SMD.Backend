package com.example.smd.dto.response.feedback;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SectionResponse {
    String sectionId;
    String title;
    Integer orderIndex;
    String actionAfter;
    String targetSectionId;
}
