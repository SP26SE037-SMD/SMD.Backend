package com.example.smd.dto.response.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriteriaLevelResponse {
    String code;
    String description;
}
