package com.example.smd.dto.request.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RubricLevelRequest {
    String levelCode;
    BigDecimal minScore;
    BigDecimal maxScore;
    String displayOrder;
}
