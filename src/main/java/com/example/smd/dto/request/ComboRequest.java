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
public class ComboRequest {
    @NotBlank(message = "COMBO_CODE_REQUIRED")
    @Size(max = 20, message = "COMBO_CODE_INVALID")
    String comboCode;

    @Size(max = 100, message = "COMBO_NAME_INVALID")
    String comboName;

    String description;

    @Size(max = 20, message = "TYPE_INVALID")
    String type; // Elective / Mandatory
}
