package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a Subject (Trạng thái vòng đời của Môn học)")
public enum SubjectStatus {

    @Schema(description = "Draft: Initial creation, subject details are being drafted")
    DRAFT,

    @Schema(description = "Defined: Subject description completed and submitted for Curriculum approval")
    DEFINED,

    @Schema(description = "Waiting Syllabus: Curriculum approved, now waiting for detailed Syllabus development")
    WAITING_SYLLABUS,

    @Schema(description = "Submitted and awaiting review assignment.")
    PENDING_REVIEW,

    @Schema(description = "Completed: Detailed Syllabus is linked, the subject is fully ready for use")
    COMPLETED,

    @Schema(description = "Archived: Subject is no longer part of active teaching, kept for historical records")
    ARCHIVED
}
