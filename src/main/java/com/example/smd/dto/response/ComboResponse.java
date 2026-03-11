package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComboResponse {
    UUID comboId;
    String comboCode;
    String comboName;
    String description;
    Instant createdAt;
    @Schema(example = "Elective / Mandatory")
    String type;
}
