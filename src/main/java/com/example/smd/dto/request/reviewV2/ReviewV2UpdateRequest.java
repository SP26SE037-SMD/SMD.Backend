package com.example.smd.dto.request.reviewV2;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewV2UpdateRequest {

    /** Cập nhật kết quả review */
    Boolean isAccepted;

    /** Cập nhật nhận xét */
    String comment;
}
