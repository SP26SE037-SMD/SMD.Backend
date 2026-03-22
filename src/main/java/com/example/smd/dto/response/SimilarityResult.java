package com.example.smd.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilarityResult {
    private String contentTitle;
    private String contentBody;
    private String chapterTitle;
    private Double distance;
    private String type;
}
