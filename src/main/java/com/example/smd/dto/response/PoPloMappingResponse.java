package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoPloMappingResponse {
    String id;
    String poId;
    String poCode;
    String ploId;
    String ploCode;
}
