package com.example.smd.dto.response.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RubricResponse {
    String code;
    String name;
    String syllabusId;
    List<CriterionResponse> criteria;
}
