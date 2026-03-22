package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of the Curriculum (Khung chương trình đào tạo)")
public enum CurriculumStatus {

    @Schema(description = "Draft: Initial creation by HoC/FDC, not visible to others")
    DRAFT,

    @Schema(description = "Structure Reviewed: Approved by Vice President (VP) to continue internal development")
    STRUCTURE_REVIEW,

    @Schema(description = "Syllabus Developing: HoC/FDC and Departments are creating detailed syllabuses")
    SYLLABUS_DEVELOPING,

    @Schema(description = "Final Review: Final overall content review by HoC/FDC before submitting to VP")
    FINAL_REVIEW,

    @Schema(description = "Signed: Officially signed and enacted by the Vice President")
    SIGNED,

    @Schema(description = "Published: Curriculum and Syllabuses are now public and viewable")
    PUBLISHED,

    @Schema(description = "Archived: Old curriculum version, no longer in use for new intakes")
    ARCHIVED
}
