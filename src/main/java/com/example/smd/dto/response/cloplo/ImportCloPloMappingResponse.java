package com.example.smd.dto.response.cloplo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCloPloMappingResponse {
    private int total;
    private int success;
    private int failed;
    private List<ImportCloPloMappingResult> details;
}
