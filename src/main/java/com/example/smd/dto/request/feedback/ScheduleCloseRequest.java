package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleCloseRequest {
    Instant closeAt;
}
