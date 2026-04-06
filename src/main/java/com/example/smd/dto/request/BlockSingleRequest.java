package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockSingleRequest {
    String blockStyle;
    String blockType;
    String contentText;
    int idx;
}
