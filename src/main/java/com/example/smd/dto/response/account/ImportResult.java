package com.example.smd.dto.response.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ImportResult {
    private int total;
    private int success;
    private int failed;

    private List<ImportAccountResult> details;

    public ImportResult(int total, int success, int failed, List<ImportAccountResult> details) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.details = details;
    }
}
