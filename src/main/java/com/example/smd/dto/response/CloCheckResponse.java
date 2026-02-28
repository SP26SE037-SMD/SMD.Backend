package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloCheckResponse {
    boolean valid;        // true/false
    String detectedVerb;  // Động từ nó tìm thấy (VD: Liệt kê)
    String detectedLevel; // Mức nó đoán (VD: Mức 1 - Nhớ)
    String suggestion;    // Lời khuyên nếu sai
}
