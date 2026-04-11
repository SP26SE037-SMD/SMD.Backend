package com.example.smd.dto.request.feedback;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateQuestionRequest {
    String content;
    String type;
    Boolean isRequired;
    Integer orderIndex;
    List<CreateOptionRequest> options;
}
