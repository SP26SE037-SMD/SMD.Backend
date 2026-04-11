package com.example.smd.dto.request.feedback;

import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFormRequest {
    String title; // Cho phép update title nếu cần, backend xử lý linh hoạt
    String formType;
    Instant closeAt;
}
