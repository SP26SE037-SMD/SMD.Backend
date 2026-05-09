package com.example.smd.dto.response.cloplo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCloPloMappingResult {
    private String subjectCode;
    private String cloCode;
    private String status;
    private String message;
}
