package com.example.smd.dto.response.source;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportSourceResult {
    String sourceCode;
    String status;
    String message;
}
