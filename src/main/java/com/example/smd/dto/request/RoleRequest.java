package com.example.smd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {
    @NotBlank(message = "ROLE_NAME_REQUIRED")
    @Size(max = 50, message = "ROLE_NAME_INVALID")
    String roleName;

    String description;

    Set<String> permissionIds;
}
