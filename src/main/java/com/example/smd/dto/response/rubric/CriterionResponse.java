package com.example.smd.dto.response.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriterionResponse {
    String code;
    String name;
    BigDecimal weight;
    List<CriteriaLevelResponse> levels;
}
