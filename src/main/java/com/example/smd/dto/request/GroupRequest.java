package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupRequest {
    @NotBlank(message = "GROUP_CODE_REQUIRED")
    @Size(max = 20, message = "GROUP_CODE_INVALID")
    String groupCode;

    @Size(max = 100, message = "GROUP_NAME_INVALID")
    String groupName;

    String description;

    @Size(max = 20, message = "TYPE_INVALID")
    String type; // Elective / Mandatory
}
