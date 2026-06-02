package com.example.smd.dto.request.rubric;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriterionRequest {
    String code;
    String criterionName;
    BigDecimal weight;
    Integer displayOrder;
    List<CriteriaLevelRequest> levels;
}
