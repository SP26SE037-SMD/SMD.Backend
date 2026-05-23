package com.example.smd.dto.response.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HopdcResponse {

    UUID   accountId;
    String email;
    String fullName;

    DepartmentDto department;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentDto {
        UUID   departmentId;
        String departmentCode;
        String departmentName;
    }
}
