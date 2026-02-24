package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PLOsResponse {
    String ploId;
    String ploCode;
    String ploName;
    String description;
    String majorId;
    String majorName;
}
