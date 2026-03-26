package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackOptionRequest {
    Integer optionNo;
    String optionText;
}
