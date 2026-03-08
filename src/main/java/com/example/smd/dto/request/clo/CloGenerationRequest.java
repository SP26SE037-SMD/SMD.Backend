package com.example.smd.dto.request.clo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloGenerationRequest {
    String subjectName; // VD: "Lập trình Java"
    String topicName;   // VD: "Vòng lặp For"
    int bloomLevel;     // VD: 3
    String descriptionPlo;
}