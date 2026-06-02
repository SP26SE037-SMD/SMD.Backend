package com.example.smd.dto.response.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RubricLevelResponse {
    UUID levelId;
    String levelCode;
    BigDecimal minScore;
    BigDecimal maxScore;
    String displayOrder;
}
