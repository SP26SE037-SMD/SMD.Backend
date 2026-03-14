package com.example.smd.dto.response.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportAccountResult {
    private String email;
    private String status; // SUCCESS | FAILED
    private String message;

    public ImportAccountResult(String email, String status, String message) {
        this.email = email;
        this.status = status;
        this.message = message;
    }
}
