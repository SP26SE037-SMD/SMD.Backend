package com.example.smd.dto.request.reviewV2;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewV2CreateRequest {

    /** ID của TaskV2 mà review này thuộc về */
    UUID taskId;

    /** ID của Account thực hiện review */
    UUID reviewerId;


    /** Nhận xét / ghi chú của reviewer */
    String comment;
}
