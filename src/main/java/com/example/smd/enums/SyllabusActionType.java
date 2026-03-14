package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Các loại hành động thực hiện trên Syllabus để lưu vết lịch sử (Logs)")
public enum SyllabusActionType {

    @Schema(description = "Khởi tạo đề cương mới")
    CREATE("Khởi tạo"),

    @Schema(description = "Cập nhật nội dung (CLOs, Sessions, Assessments,...)")
    UPDATE("Cập nhật"),

    @Schema(description = "Gửi đề cương đi để chờ phê duyệt")
    SUBMIT("Gửi duyệt"),

    @Schema(description = "Phân công người hoặc hội đồng thẩm định")
    ASSIGN_REVIEW("Phân công Review"),

    @Schema(description = "Reviewer bắt đầu quá trình đánh giá")
    START_REVIEW("Bắt đầu Review"),

    @Schema(description = "Yêu cầu người soạn thảo chỉnh sửa lại nội dung")
    REQUEST_REVISION("Yêu cầu chỉnh sửa"),

    @Schema(description = "Chấp nhận nội dung đề cương (Về mặt chuyên môn)")
    APPROVE("Phê duyệt"),

    @Schema(description = "Từ chối đề cương")
    REJECT("Từ chối"),

    @Schema(description = "Ban hành đề cương để áp dụng chính thức")
    PUBLISH("Ban hành"),

    @Schema(description = "Đưa đề cương vào kho lưu trữ (Hết hiệu lực)")
    ARCHIVE("Lưu trữ");

    private final String description;

    SyllabusActionType(String description) {
        this.description = description;
    }
}
