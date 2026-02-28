package com.example.smd.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloCheckRequest {
    String cloContent; // Ví dụ: "Liệt kê các thẻ HTML"
    int targetLevel;   // Ví dụ: 6 (Sáng tạo)
}
