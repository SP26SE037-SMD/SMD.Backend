package com.example.smd.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // Authentication & Authorization
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1003, "Invalid username or password", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1004, "Password and confirm password do not match", HttpStatus.BAD_REQUEST),

    // Account
    ACCOUNT_NOT_FOUND(2001, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_EXISTS(2002, "Account already exists", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTS(2003, "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS(2004, "Email already exists", HttpStatus.BAD_REQUEST),

    // Role
    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_EXISTS(3002, "Role already exists", HttpStatus.BAD_REQUEST),

    // Permission
    PERMISSION_NOT_FOUND(4001, "Permission not found", HttpStatus.NOT_FOUND),
    PERMISSION_EXISTS(4002, "Permission already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_LIST_REQUIRED(4003, "Permission list is required", HttpStatus.BAD_REQUEST),

    // Major
    MAJOR_NOT_FOUND(5001, "Major not found", HttpStatus.NOT_FOUND),
    MAJOR_CODE_EXISTS(5002, "Major code already exists", HttpStatus.BAD_REQUEST),
    MAJOR_CODE_REQUIRED(5003, "Major code is required", HttpStatus.BAD_REQUEST),
    MAJOR_NAME_REQUIRED(5004, "Major name is required", HttpStatus.BAD_REQUEST),
    MAJOR_CODE_TOO_LONG(5005, "Major code must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    INVALID_MAJOR_STATUS(5006, "Invalid Major status transition", HttpStatus.BAD_REQUEST),
    MAJOR_NOT_DRAFT(5007, "Major can only be modified when in DRAFT status", HttpStatus.BAD_REQUEST),

    //PLOs
    PLO_NOT_FOUND(6001, "PLO not found", HttpStatus.NOT_FOUND),
    PLO_CODE_EXISTS(6002, "PLO code already exists in this curriculum", HttpStatus.BAD_REQUEST),
    PLO_CODE_REQUIRED(6003, "PLO code is required", HttpStatus.BAD_REQUEST),
    PLO_IN_USE(6004, "Cannot delete PLO because it is currently linked to courses", HttpStatus.CONFLICT),
    INVALID_PLO_STATUS(6005, "Invalid PLO status transition", HttpStatus.BAD_REQUEST),
    PLO_NOT_DRAFT(6006, "PLO can only be modified when in DRAFT status", HttpStatus.BAD_REQUEST),

    //CLOs
    // CLO (Course Learning Outcomes)
    CLO_NOT_FOUND(7001, "CLO not found", HttpStatus.NOT_FOUND),
    CLO_CODE_EXISTS(7002, "CLO code already exists in this syllabus", HttpStatus.BAD_REQUEST),
    CLO_CODE_REQUIRED(7003, "CLO code is required", HttpStatus.BAD_REQUEST),
    CLO_NAME_REQUIRED(7004, "CLO name is required", HttpStatus.BAD_REQUEST),
    SYLLABUS_ID_REQUIRED(7005, "Syllabus (Subject) ID is required", HttpStatus.BAD_REQUEST),
    BLOOM_LEVEL_REQUIRED(7006, "Bloom level is required and must be between 1-6", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_INPUT(7007, "Invalid status provided. Please check the allowed list.", HttpStatus.BAD_REQUEST),
    INVALID_CLO_STATUS(7008, "Invalid CLO status transition",
            HttpStatus.BAD_REQUEST),
    CLO_SUBJECT_NOT_EDITABLE(7009, "CLO can edit when subject status in WAITING_SYLLABUS  status", HttpStatus.BAD_REQUEST),
    CLO_NOT_EDITABLE(7010, "CLO can only be modified when the subject is in DRAFT status", HttpStatus.BAD_REQUEST),

    // Subject
    SUBJECT_NOT_FOUND(8001, "Subject not found", HttpStatus.NOT_FOUND),
    SUBJECT_CODE_EXISTS(8002, "Subject code already exists", HttpStatus.BAD_REQUEST),
    DECISION_NO_REQUIRED(8003, "Decision number cannot be blank", HttpStatus.BAD_REQUEST),
    SUBJECT_CODE_REQUIRED(8004, "Subject code cannot be blank", HttpStatus.BAD_REQUEST),
    SUBJECT_NAME_REQUIRED(8005, "Subject name cannot be blank", HttpStatus.BAD_REQUEST),
    INVALID_SUBJECT_STATUS(8006, "Invalid subject status. Please follow the workflow: DRAFT -> DEFINED -> WAITING_SYLLABUS -> COMPLETED.", HttpStatus.BAD_REQUEST),
    SUBJECT_NOT_DRAFT(8007, "Subject can only be modified when in DRAFT status", HttpStatus.BAD_REQUEST),
    SUBJECT_NOT_CREATE(8008, "Create Subject for an DRAFT Curriculum", HttpStatus.BAD_REQUEST),

    // Gemini
    // AI & External Services (9xxx)
    AI_GENERATION_FAILED(9001, "AI failed to generate valid content, please try again", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_RESPONSE_INVALID_FORMAT(9002, "AI response is not in a valid JSON format", HttpStatus.UNPROCESSABLE_ENTITY),
    GEMINI_API_ERROR(9003, "Error occurred while calling Gemini API", HttpStatus.BAD_GATEWAY),
    AI_PROVIDER_ERROR(9004, "AI service connection or processing error (Gemini)", HttpStatus.SERVICE_UNAVAILABLE),
    AI_QUOTA_EXCEEDED(9005, "AI API rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    EMBEDDING_FAILED(9006, "Vector embedding generation failed", HttpStatus.UNPROCESSABLE_ENTITY),

    // Elective
    ELECTIVE_CODE_REQUIRED(11001, "Elective code cannot be blank", HttpStatus.BAD_REQUEST),
    ELECTIVE_NAME_REQUIRED(11002, "Elective name cannot be blank", HttpStatus.BAD_REQUEST),
    ELECTIVE_NOT_FOUND(11003, "Elective group not found", HttpStatus.NOT_FOUND),
    ELECTIVE_CODE_EXISTED(11004, "Elective code already exists", HttpStatus.BAD_REQUEST),
    ELECTIVE_HAS_SUBJECTS(11005, "Cannot delete elective group that contains subjects", HttpStatus.CONFLICT),
    MIN_CREDITS_INVALID(11006, "Minimum credits required must be a positive number", HttpStatus.BAD_REQUEST),
    ELECTIVE_SUBJECT_ALREADY_EXISTS(11007, "This subject is already assigned to the elective group", HttpStatus.BAD_REQUEST),
    ELECTIVE_SUBJECT_NOT_FOUND(111008, "The connection between this subject and elective group does not exist", HttpStatus.NOT_FOUND),

    //Department
    DEPARTMENT_CODE_REQUIRED(12001, "Department code cannot be blank", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_REQUIRED(12002, "Department name cannot be blank", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(12003, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_CODE_EXISTED(12004, "Department code already exists", HttpStatus.BAD_REQUEST),
    DEPARTMENT_HAS_CONSTRAINTS(12005, "Cannot delete department with linked subjects or lecturers", HttpStatus.CONFLICT),
    DEPARTMENT_CODE_CANNOT_BE_CHANGED(12006, "Department code cannot be modified after creation", HttpStatus.BAD_REQUEST),

    //PREREQUISITE
    PREREQUISITE_NOT_FOUND(13001, "Prerequisite relationship not found", HttpStatus.NOT_FOUND),
    PREREQUISITE_ALREADY_EXISTS(13002, "This prerequisite relationship already exists", HttpStatus.BAD_REQUEST),
    PREREQUISITE_SELF_REFERENCE(13003, "A subject cannot be its own prerequisite", HttpStatus.BAD_REQUEST),
    SUBJECT_ID_REQUIRED(13004, "Subject ID is required", HttpStatus.BAD_REQUEST),
    PREREQUISITE_ID_REQUIRED(13005, "Prerequisite subject ID is required", HttpStatus.BAD_REQUEST),
    PREREQUISITE_CYCLE_DETECTED(13006, "Circular dependency detected: These subjects cannot be prerequisites of each other", HttpStatus.BAD_REQUEST),
    PREREQUISITE_NOT_DEFINED(13007, "Subject must not be in DRAFT status to update prerequisites", HttpStatus.BAD_REQUEST),

    //CURRICULUM
    CURRICULUM_NOT_FOUND(10001, "Curriculum not found", HttpStatus.NOT_FOUND),
    CURRICULUM_CODE_EXISTS(10002, "Curriculum code already exists", HttpStatus.BAD_REQUEST),
    CURRICULUM_CODE_REQUIRED(10003, "Curriculum code is required", HttpStatus.BAD_REQUEST),
    CURRICULUM_NAME_REQUIRED(10004, "Curriculum name is required", HttpStatus.BAD_REQUEST),
    CURRICULUM_CODE_TOO_LONG(10005, "Curriculum code must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    CURRICULUM_NAME_TOO_LONG(10006, "Curriculum name must not exceed 100 characters", HttpStatus.BAD_REQUEST),
    MAJOR_ID_REQUIRED(10007, "Major ID is required", HttpStatus.BAD_REQUEST),
    INVALID_YEAR_RANGE(10008, "End year must be greater than start year", HttpStatus.BAD_REQUEST),
    CURRICULUM_HAS_SUBJECTS(10009, "Cannot delete curriculum that contains subjects", HttpStatus.CONFLICT),
    INVALID_CURRICULUM_STATUS(10010, "Invalid curriculum status transition", HttpStatus.BAD_REQUEST),
    CURRICULUM_NOT_DRAFT(10011, "Curriculum can only be modified when in DRAFT status", HttpStatus.BAD_REQUEST),
    CURRICULUM_NOT_CREATE(10012, "Cannot create Curriculum for an ARCHIVED Major and DRAFT Major", HttpStatus.BAD_REQUEST),

    //System Log
    LOG_NOT_FOUND(14001, "System log not found", HttpStatus.NOT_FOUND),
    ACTION_REQUIRED(14002, "Action is required", HttpStatus.BAD_REQUEST),
    ACTION_TOO_LONG(14003, "Action must not exceed 100 characters", HttpStatus.BAD_REQUEST),

    //Notification
    NOTIFICATION_NOT_FOUND(15001, "Notification not found", HttpStatus.NOT_FOUND),
    TITLE_REQUIRED(15002, "Notification title cannot be blank", HttpStatus.BAD_REQUEST),
    MESSAGE_REQUIRED(15003, "Notification message cannot be blank", HttpStatus.BAD_REQUEST),
    TYPE_REQUIRED(15004, "Notification type is required", HttpStatus.BAD_REQUEST),
    ACCOUNT_ID_REQUIRED(15005, "Account ID is required for notification", HttpStatus.BAD_REQUEST),

    //CLO_PLO_MAPPING
    CLO_PLO_MAPPING_NOT_FOUND(16001, "CLO-PLO Mapping not found", HttpStatus.NOT_FOUND),
    MAPPING_ALREADY_EXISTS(16002, "This CLO is already mapped to this PLO", HttpStatus.BAD_REQUEST),
    INVALID_CONTRIBUTION_LEVEL(16003, "Contribution level must be Low, Medium, or High", HttpStatus.BAD_REQUEST),

    //ACCOUNT_PROFILE
    ACCOUNT_PROFILE_NOT_FOUND(17001, "Account profile not found", HttpStatus.NOT_FOUND),
    PHONE_NUMBER_INVALID(17002, "Phone number must be 10-11 digits"
            , HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_OWNER(17003, "You can only access or modify your own profile", HttpStatus.FORBIDDEN),

    //GROUP
    GROUP_NOT_FOUND(19001, "Group not found", HttpStatus.NOT_FOUND),
    GROUP_CODE_EXISTS(19002, "Group code already exists", HttpStatus.BAD_REQUEST),
    GROUP_CODE_REQUIRED(19003, "Group code is required", HttpStatus.BAD_REQUEST),
    GROUP_NAME_REQUIRED(19004, "Group name is required", HttpStatus.BAD_REQUEST),

    //SYLLABUS
    SYLLABUS_NOT_FOUND(18001, "The syllabus does not exist on the system.", HttpStatus.NOT_FOUND),
    INVALID_SYLLABUS_STATUS(18002, "The syllabus status is invalid.", HttpStatus.BAD_REQUEST),
    SYLLABUS_NOT_EDITABLE(18003, "Syllabus can only be modified when in DRAFT or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),
    SYLLABUS_NOT_CREATE(18004, "Subject must be in WAITING_SYLLABUS or COMPLETED status to create a Syllabus", HttpStatus.BAD_REQUEST),

    //CURRICULUM_GROUP_SUBJECT
    CURRICULUM_GROUP_SUBJECT_ALREADY_EXISTS(20001, "This subject is already added to this curriculum", HttpStatus.BAD_REQUEST),
    CURRICULUM_GROUP_SUBJECT_NOT_FOUND(20002, "Curriculum-Group-Subject mapping not found", HttpStatus.NOT_FOUND),
    CURRICULUM_GROUP_SUBJECT_NOT_CREATE(20003, "Cannot modify Curriculum-Group-Subject mapping unless Curriculum and Subject is in DRAFT status", HttpStatus.BAD_REQUEST),

    //POs
    PO_NOT_FOUND(21001, "Program Outcome (PO) not found", HttpStatus.NOT_FOUND),
    PO_CODE_EXISTS(21002, "PO Code already exists in this Major", HttpStatus.BAD_REQUEST),
    PO_LIST_EMPTY(21003, "The provided PO list is empty", HttpStatus.BAD_REQUEST),
    PO_IN_USE(21004, "PO is currently mapped to PLOs and cannot be deleted", HttpStatus.CONFLICT),
    INVALID_PO_STATUS(21005, "Invalid PO status transition", HttpStatus.BAD_REQUEST),
    PO_NOT_DRAFT(21006, "PO can only be modified when in DRAFT status", HttpStatus.BAD_REQUEST),

    // PO_PLO_MAPPING
    PO_PLO_MAPPING_NOT_FOUND(22001, "PO-PLO mapping not found", HttpStatus.NOT_FOUND),
    PO_PLO_MAPPING_EXISTS(22002, "This PLO is already mapped to this PO", HttpStatus.BAD_REQUEST),
    MAPPING_LIST_REQUIRED(22003, "Mapping list is required", HttpStatus.BAD_REQUEST),

    // SPRINT
    SPRINT_NOT_FOUND(23001, "Sprint not found", HttpStatus.NOT_FOUND),
    SPRINT_NAME_REQUIRED(23002, "Sprint name is required", HttpStatus.BAD_REQUEST),
    SPRINT_NAME_TOO_LONG(23003, "Sprint name must not exceed 100 characters", HttpStatus.BAD_REQUEST),
    STATUS_TOO_LONG(23004, "Status must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    INVALID_SPRINT_STATUS(23005, "Invalid sprint status. Allowed values: Planning, Active, Completed", HttpStatus.BAD_REQUEST),

    // TASK
    TASK_NOT_FOUND(25001, "Task not found", HttpStatus.NOT_FOUND),
    TASK_NAME_REQUIRED(25002, "Task name is required", HttpStatus.BAD_REQUEST),
    TASK_NAME_TOO_LONG(25003, "Task name must not exceed 150 characters", HttpStatus.BAD_REQUEST),
    SPRINT_ID_REQUIRED(25004, "Sprint ID is required", HttpStatus.BAD_REQUEST),
    PRIORITY_TOO_LONG(25005, "Priority must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    TASK_LIST_REQUIRED(25006, "Task list is required", HttpStatus.BAD_REQUEST),
    INVALID_TASK_STATUS(25007, "Invalid task status. Allowed values: To Do, In Progress, In Review, Done, Blocked, Cancelled", HttpStatus.BAD_REQUEST),
    TASK_TYPE_TOO_LONG(25008, "Task type must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    SYLLABUS_NOT_READY_FOR_TASK(25009, "Tasks can only be assigned when Syllabus is IN_PROGRESS or PENDING_REVIEW", HttpStatus.BAD_REQUEST),
    CURRICULUM_NOT_READY_FOR_TASK(25010, "Tasks can only be assigned after Curriculum structure is APPROVED", HttpStatus.BAD_REQUEST),
    TASK_NOT_EDITABLE(25011, "Tasks can only be modified when in TO_DO status", HttpStatus.BAD_REQUEST),
    TASK_SUBJECT_ID_REQUIRED(25012, "Task subject ID is required", HttpStatus.BAD_REQUEST),
    CURRICULUM_ID_REQUIRED(25013, "Curriculum ID is required", HttpStatus.BAD_REQUEST),

    // SOURCE
    SOURCE_NOT_FOUND(24001, "Source not found", HttpStatus.NOT_FOUND),
    INVALID_SOURCE_TYPE(24002, "Invalid source type. Please choose from: TEXTBOOK, REFERENCE_BOOK, ONLINE_COURSE, DOCUMENTATION, JOURNAL_PAPER, ARTICLE", HttpStatus.BAD_REQUEST),
    INVALID_YEAR(24003, "Published year must be between 1900 and the current year", HttpStatus.BAD_REQUEST),
    MAPPING_NOT_FOUND(24004, "Syllabus and Source mapping not found", HttpStatus.NOT_FOUND),
    SOURCE_ALREADY_MAPPED(24005, "Source is already assigned to this syllabus", HttpStatus.CONFLICT),

    // ASSESSMENT
    ASSESSMENT_NOT_FOUND(26001, "Assessment not found", HttpStatus.NOT_FOUND),
    ASSESSMENT_CATEGORY_NOT_FOUND(26002, "Assessment category not found", HttpStatus.NOT_FOUND),
    ASSESSMENT_TYPE_NOT_FOUND(26003, "Assessment type not found", HttpStatus.NOT_FOUND),
    ASSESSMENT_WEIGHT_EXCEEDS_LIMIT(26004, "Total assessment weight in a syllabus must not exceed 100", HttpStatus.BAD_REQUEST),
    ASSESSMENT_WEIGHT_INVALID(26005, "Assessment weight must be greater than 0 and less than or equal to 100", HttpStatus.BAD_REQUEST),
    ASSESSMENT_STATUS_REQUIRED(26006, "Assessment status is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_CATEGORY_ID_REQUIRED(26007, "Assessment category ID is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TYPE_ID_REQUIRED(26008, "Assessment type ID is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_PART_REQUIRED(26009, "Assessment part is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_PART_INVALID(26010, "Assessment part must be greater than or equal to 1", HttpStatus.BAD_REQUEST),
    ASSESSMENT_WEIGHT_REQUIRED(26011, "Assessment weight is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_DURATION_INVALID(26012, "Assessment duration must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    ASSESSMENT_QUESTION_TYPE_INVALID(26013, "Question type must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    ASSESSMENT_KNOWLEDGE_SKILL_INVALID(26014, "Knowledge/skill must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    ASSESSMENT_STATUS_INVALID(26015, "Assessment status must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    ASSESSMENT_CATEGORY_EXISTS(26016, "Assessment category already exists", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TYPE_EXISTS(26017, "Assessment type already exists", HttpStatus.BAD_REQUEST),
    ASSESSMENT_CATEGORY_NAME_REQUIRED(26018, "Assessment category name is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_CATEGORY_NAME_INVALID(26019, "Assessment category name must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TYPE_NAME_REQUIRED(26020, "Assessment type name is required", HttpStatus.BAD_REQUEST),
    ASSESSMENT_TYPE_NAME_INVALID(26021, "Assessment type name must not exceed 50 characters", HttpStatus.BAD_REQUEST),
    ASSESSMENT_NOT_EDITABLE(26022, "Assessment can only be modified when in DRAFT or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),
    ASSESSMENT_CANNOT_CREATE(26023, "Assessment can only be added when Syllabus is in IN_PROGRESS or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),

    //Material
    MATERIAL_NOT_FOUND(25001, "Material not found", HttpStatus.NOT_FOUND),
    INVALID_MATERIAL_STATUS(25002, "Invalid material status", HttpStatus.BAD_REQUEST),
    MATERIAL_NOT_EDITABLE(25003, "Material can only be modified when in DRAFT or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),
    MATERIAL_CANNOT_CREATE(25004, "Materials can only be added when Syllabus is in IN_PROGRESS or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),

    //Blocks
    BLOCK_NOT_FOUND(27001, "Block content could not be located", HttpStatus.NOT_FOUND),
    BLOCK_LIST_EMPTY(27002, "Block list cannot be empty for bulk creation", HttpStatus.BAD_REQUEST),
    INVALID_BLOCK_STYLE(27003, "The provided block style is not supported", HttpStatus.BAD_REQUEST),

    // SESSION
    SESSION_NOT_FOUND(28001, "Session not found", HttpStatus.NOT_FOUND),
    SESSION_NUMBER_REQUIRED(28002, "Session number is required", HttpStatus.BAD_REQUEST),
    SESSION_NUMBER_INVALID(28003, "Session number must be greater than or equal to 1", HttpStatus.BAD_REQUEST),
    SESSION_TITLE_REQUIRED(28004, "Session title is required", HttpStatus.BAD_REQUEST),
    SESSION_TITLE_INVALID(28005, "Session title must not exceed 200 characters", HttpStatus.BAD_REQUEST),
    SESSION_DURATION_INVALID(28006, "Session duration must be greater than or equal to 0", HttpStatus.BAD_REQUEST),
    SESSION_STATUS_REQUIRED(28007, "Session status is required", HttpStatus.BAD_REQUEST),
    SESSION_NUMBER_EXISTS(28008, "Session number already exists in this syllabus", HttpStatus.BAD_REQUEST),
    SESSION_LIST_REQUIRED(28009, "Session list is required", HttpStatus.BAD_REQUEST),
    SESSION_NOT_DRAFT(28010, "Session can only be overwritten when status is Draft", HttpStatus.BAD_REQUEST),
    SESSION_NUMBER_LIST_REQUIRED(28011, "Session number list is required", HttpStatus.BAD_REQUEST),
    SESSION_NOT_EDITABLE(28012, "Session can only be modified when in DRAFT or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),
    SESSION_CANNOT_CREATE(28013, "Session can only be added when Syllabus is in IN_PROGRESS or REVISION_REQUESTED status", HttpStatus.BAD_REQUEST),

    // FEEDBACK
    FEEDBACK_QUESTION_NOT_FOUND(29001, "Feedback question not found", HttpStatus.NOT_FOUND),
    FEEDBACK_OPTION_NOT_FOUND(29002, "Feedback option not found", HttpStatus.NOT_FOUND),
    FEEDBACK_SUBMISSION_NOT_FOUND(29003, "Feedback submission not found", HttpStatus.NOT_FOUND),
    FEEDBACK_QUESTION_REQUIRED(29004, "Feedback question request is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_QUESTION_TEXT_REQUIRED(29005, "Question text is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_QUESTION_TYPE_REQUIRED(29006, "Question type is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_FORM_TYPE_REQUIRED(29007, "Form type is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_QUESTION_LIST_REQUIRED(29008, "Feedback question list is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_OPTION_REQUIRED(29009, "Feedback option request is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_OPTION_TEXT_REQUIRED(29010, "Option text is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_OPTION_LIST_REQUIRED(29011, "Feedback option list is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_SUBMISSION_REQUIRED(29012, "Feedback submission request is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_SUBMISSION_LIST_REQUIRED(29013, "Feedback submission list is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_ANSWER_LIST_REQUIRED(29014, "Feedback answer list is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_QUESTION_ID_REQUIRED(29015, "Question ID is required for each feedback answer", HttpStatus.BAD_REQUEST),
    FEEDBACK_ANSWER_INVALID(29016, "Answer must contain selected option or answer text", HttpStatus.BAD_REQUEST),
    FEEDBACK_QUESTION_IN_USE(29017, "Cannot delete question because it has feedback answers", HttpStatus.CONFLICT),
    FEEDBACK_OPTION_IN_USE(29018, "Cannot delete option because it has feedback answers", HttpStatus.CONFLICT),
    FEEDBACK_CURRICULUM_ID_REQUIRED(29019, "Curriculum ID is required", HttpStatus.BAD_REQUEST),
    FEEDBACK_FORM_NOT_FOUND(29020, "Feedback form not found", HttpStatus.NOT_FOUND),
    FEEDBACK_SECTION_NOT_FOUND(29021, "Feedback form section not found", HttpStatus.NOT_FOUND),
    APP_SCRIPT_CALL_FAILED(29022, "Failed to call App Script service", HttpStatus.BAD_GATEWAY),

    // REVIEW TASK
    REVIEW_TASK_NOT_FOUND(30001, "Review task not found", HttpStatus.NOT_FOUND),
    REVIEW_TASK_REQUEST_REQUIRED(30002, "Review task request is required", HttpStatus.BAD_REQUEST),
    REVIEW_TASK_TASK_ID_REQUIRED(30003, "Task ID is required", HttpStatus.BAD_REQUEST),
    REVIEW_TASK_STATUS_REQUIRED(30004, "Review task status is required", HttpStatus.BAD_REQUEST),

    // REQUEST
    REQUEST_NOT_FOUND(31001, "Request not found", HttpStatus.NOT_FOUND),
    REQUEST_TITLE_REQUIRED(31002, "Request title is required", HttpStatus.BAD_REQUEST),
    REQUEST_TITLE_TOO_LONG(31003, "Request title must not exceed 50 characters", HttpStatus.BAD_REQUEST),

    // General
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1000, "Invalid message key", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED_FOR_ROLE(8888, "Your role does not have permission to view this content", HttpStatus.FORBIDDEN),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
