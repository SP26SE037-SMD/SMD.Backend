package com.example.smd.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data // Tự động tạo Getter, Setter, toString...
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MajorResponse {
    private String majorId;
    private String majorCode;
    private String majorName;
    private String description;
    private Boolean status;
}
