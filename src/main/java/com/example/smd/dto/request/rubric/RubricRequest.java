package com.example.smd.dto.request.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RubricRequest {
    String code;
    String name;
    String syllabusId;
    List<CriterionRequest> criteria;
}
