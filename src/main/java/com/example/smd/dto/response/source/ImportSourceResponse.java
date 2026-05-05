package com.example.smd.dto.response.source;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportSourceResponse {
    int total;
    int success;
    int failed;
    List<ImportSourceResult> details;
}
