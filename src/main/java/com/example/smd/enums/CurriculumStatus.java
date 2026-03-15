package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái vòng đời của Khung chương trình đào tạo (Curriculum)")
public enum CurriculumStatus {

    @Schema(description = "Biên soạn: HoC/FDC đang thiết kế cấu trúc khung chương trình và danh mục môn học")
    DRAFT,

    @Schema(description = "Rà soát nội bộ (Chưa ban hành): HoP chốt danh sách môn học nhưng chưa có quyết định phê duyệt từ VP")
    INTERNAL_REVIEW_WITHOUT_ENACTMENT,

    @Schema(description = "Rà soát nội bộ (Đã ban hành): VP đã phê duyệt khung, cho phép các bộ môn triển khai syllabus chi tiết")
    INTERNAL_REVIEW_WITH_ENACTMENT,

    @Schema(description = "Đã xuất bản: Khung chương trình chính thức được áp dụng cho khóa tuyển sinh")
    PUBLISHED,

    @Schema(description = "Lưu trữ: Khung chương trình cũ, không còn áp dụng cho các khóa mới")
    ARCHIVED,
}