package com.example.smd.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoPloCurriculumResponse {
    String poId;
    String poCode;
    String descriptionPo;
    String ploId;
    String ploCode;
    String descriptionPlo;
}
