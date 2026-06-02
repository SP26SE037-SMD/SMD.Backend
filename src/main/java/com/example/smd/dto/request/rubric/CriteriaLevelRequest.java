package com.example.smd.dto.request.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriteriaLevelRequest {
    String levelCode;
    String description;
}
