package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockWithIdxRequest {
    String blockStyle;
    String blockType;
    String contentText;
    Integer idx; // Số thứ tự do client chỉ định (không tự tính như BlockRequest)
}
