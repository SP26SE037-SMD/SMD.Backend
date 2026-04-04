package com.example.smd.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Các loại hành động thực hiện trên Syllabus để lưu vết lịch sử (Logs)")
public enum SyllabusActionType {

    @Schema(description = "Create a new syllabus record in the system")
    CREATE("created a new syllabus record in the system"),

    @Schema(description = "Update general information such as CLOs, Credits, and Descriptions")
    UPDATE("updated general information such as CLOs, Credits, and Descriptions"),

    @Schema(description = "Develop detailed content including sessions and learning materials")
    DEVELOP("developed detailed content including sessions and learning materials"),

    @Schema(description = "Submit the syllabus to the department for formal review")
    SUBMIT("submitted the syllabus to the department for formal review"),

    @Schema(description = "Assign a specific reviewer or committee to evaluate the syllabus")
    ASSIGN_REVIEW("assigned a specific reviewer or committee to evaluate the syllabus"),

    @Schema(description = "Start the formal evaluation process by the assigned reviewer")
    START_REVIEW("start the formal evaluation process by the assigned reviewer"),

    @Schema(description = "Request the author to revise content based on evaluation feedback")
    REQUEST_REVISION("requested the author to revise content based on evaluation feedback"),

    @Schema(description = "Approve the syllabus content as technically and academically sound")
    APPROVE("approved the syllabus content as technically and academically sound"),

    @Schema(description = "Reject the syllabus and stop the current approval workflow")
    REJECT("rejected the syllabus content as technically and academically sound"),

    @Schema(description = "Publish the syllabus for official use and portal display")
    PUBLISH("Published the syllabus for official use and portal display"),

    @Schema(description = "Archive the syllabus when it is no longer in effect")
    ARCHIVE("Archived the syllabus when it is no longer in effect");

    private final String description;

    SyllabusActionType(String description) {
        this.description = description;
    }
}
