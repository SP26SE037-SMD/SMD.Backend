package com.example.smd.dto.response;

import com.example.smd.enums.ImpactStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImpactResponse {
    String removeContent;
    ImpactStatus impactStatus;
    Double similarity;
}
