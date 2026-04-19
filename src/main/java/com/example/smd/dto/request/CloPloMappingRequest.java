package com.example.smd.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloPloMappingRequest {
    @NotNull(message = "CLO ID is required")
    UUID cloId;

    @NotNull(message = "PLO ID is required")
    UUID ploId;

    @Pattern(regexp = "Low|Medium|High", message = "Level must be Low, Medium, or High")
    String contributionLevel;
}