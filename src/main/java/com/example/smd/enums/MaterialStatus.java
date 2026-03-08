package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum MaterialStatus {
    @Schema(description = "Đã gửi và đang chờ được phân công review")
    PENDING_REVIEW,

    @Schema(description = "Đang trong quá trình đánh giá bởi hội đồng/chuyên gia")
    IN_REVIEW,

    @Schema(description = "Bị phản hồi và yêu cầu người soạn thảo chỉnh sửa lại")
    REVISION_REQUESTED,

    @Schema(description = "Đã được thông qua về mặt nội dung")
    APPROVED,

    @Schema(description = "Bị từ chối (Không đạt yêu cầu hệ thống)")
    REJECTED,

    @Schema(description = "Đã chính thức ban hành và áp dụng cho sinh viên")
    PUBLISHED,
}
