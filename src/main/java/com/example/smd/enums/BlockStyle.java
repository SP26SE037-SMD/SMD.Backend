package com.example.smd.enums;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Định dạng hiển thị của từng khối nội dung trong tài liệu")
public enum BlockStyle {

    @Schema(description = "Tiêu đề chính (Heading 1)")
    H1,

    @Schema(description = "Tiêu đề phụ (Heading 2)")
    H2,

    @Schema(description = "Đoạn văn bản thông thường")
    PARAGRAPH,

    @Schema(description = "Danh sách có thứ tự (1, 2, 3...)")
    ORDERED_LIST,

    @Schema(description = "Danh sách không thứ tự (Bullet points)")
    BULLET_LIST,

    @Schema(description = "Khối mã nguồn (Code snippet)")
    CODE_BLOCK,

    @Schema(description = "Đoạn trích dẫn (Blockquote)")
    QUOTE,

    @Schema(description = "Bảng dữ liệu (Dạng chuỗi JSON hoặc Markdown)")
    TABLE,

    @Schema(description = "Đường kẻ phân cách")
    DIVIDER
}
