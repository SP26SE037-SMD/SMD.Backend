package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOptionRequest {
    String optionText;
    UUID nextSectionId;
}
