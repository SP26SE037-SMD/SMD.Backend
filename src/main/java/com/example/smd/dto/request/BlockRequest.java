package com.example.smd.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockRequest {
    String blockStyle;
    String contentText;
    // idx sẽ được hệ thống tự tính dựa trên thứ tự trong mảng gửi lên
}
