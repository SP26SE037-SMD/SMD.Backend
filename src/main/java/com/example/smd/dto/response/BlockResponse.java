package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockResponse {
    UUID blockId;
    Integer idx;
    String blockStyle;
    String blockType;
    String contentText;
}
