        package com.example.smd.controller;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.AssessmentDiffResponse;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SessionDiffResponse;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.dto.response.validate.CompareSyllabusResponse;
import com.example.smd.entities.SyllabusComparisonHistory;
import com.example.smd.enums.SyllabusActionType;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.services.AccountService;
import com.example.smd.services.EmbeddingService;
import com.example.smd.services.SyllabusActionLogService;
import com.example.smd.services.SyllabusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syllabus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Syllabus", description = "Endpoints for managing course syllabuses and their review lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class SyllabusController {

        SyllabusService syllabusService;
        SyllabusActionLogService syllabusActionLogService;
        EmbeddingService embeddingService;
        AccountService accountService;

        @PostMapping("/account/{email}")
        @Operation(summary = "Create a new syllabus", description = "Initializes a syllabus for a specific subject with status 'DRAFT'")
        @PreAuthorize("hasAuthority('SYLLABUS_CREATE')")
        public ResponseObject<SyllabusResponse> create(
                        @RequestBody @Valid SyllabusRequest request,
                        @PathVariable String email,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                SyllabusResponse response = syllabusService.create(request, userId);

                SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
                logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
                logRequest.setActionByEmail(email); // Truyền email vào đây
                logRequest.setActionType(SyllabusActionType.CREATE.toString());
                logRequest.setNote("Hệ thống: Khởi tạo đề cương mới cho môn học.");

                // 4. Lưu Log
                syllabusActionLogService.createLog(logRequest);

                return ResponseObject.<SyllabusResponse>builder()
                                .data(response)
                                .message("Syllabus created successfully")
                                .build();
        }

        @GetMapping("/subject/{subjectId}")
        @Operation(summary = "Update Syllabus Lifecycle Status (Cập nhật trạng thái Đề cương)", description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n"
                        +
                        "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ Session, Assessment, Material và CLO:\n\n"
                        +
                        "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                        "| :--- | :--- | :--- |\n" +
                        "| **DRAFT** | **Khởi tạo:** HoPDC mới tạo khung đề cương, chưa có nội dung chi tiết. | Chỉ người tạo nhìn thấy. |\n"
                        +
                        "| **IN_PROGRESS** | **Đang biên soạn:** Giảng viên đang xây dựng CLO, Session, chọn lọc học liệu và thiết kế Assessment. | Cho phép sửa nội dung chi tiết. |\n"
                        +
                        "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện, đang đợi Hội đồng/HoD phân công Reviewer thẩm định. | Khóa chỉnh sửa tạm thời. |\n"
                        +
                        "| **REVISION_REQUESTED**| **Yêu cầu chỉnh sửa:** Reviewer đã gửi Feedback. Giảng viên cần cập nhật nội dung theo yêu cầu. | Mở lại quyền chỉnh sửa. |\n"
                        +
                        "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa toàn bộ nội dung. |\n"
                        +
                        "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên bắt đầu được xem tài liệu và CLO. | Khóa vĩnh viễn (Read-only). |\n"
                        +
                        "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản), giữ lại để tra cứu lịch sử đào tạo. | Ẩn khỏi danh sách hiện hành. |\n\n")
        public ResponseObject<List<SyllabusResponse>> getAllBySubject(
                        @PathVariable UUID subjectId,
                        @Parameter(description = "Filter by status (DRAFT, INTERNAL_REVIEW, PUBLISHED, ARCHIVED)") @RequestParam(required = false) String status,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<List<SyllabusResponse>>builder()
                                .status(1000)
                                .data(syllabusService.getAllBySubject(subjectId, status, userId))
                                .message("Syllabuses retrieved successfully for subject: " + subjectId)
                                .build();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get syllabus detail", description = "Retrieves full details of a syllabus including sessions and assessments")
        public ResponseObject<SyllabusResponse> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<SyllabusResponse>builder()
                                .data(syllabusService.getDetail(id, userId))
                                .message("Syllabus detail retrieved successfully")
                                .build();
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update syllabus information", description = "Updates metadata like bloom level and name. Only allowed for DRAFT status.")
        @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
        public ResponseObject<SyllabusResponse> update(
                        @PathVariable UUID id,
                        @RequestBody @Valid SyllabusRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<SyllabusResponse>builder()
                                .data(syllabusService.update(id, request, userId))
                                .message("Syllabus updated successfully")
                                .build();
        }

        @PatchMapping("/{id}/account/{accountId}/status")
        @PreAuthorize("hasAuthority('SYLLABUS_UPDATE_STATUS')")
        @Operation(summary = "Update Syllabus Lifecycle Status (Cập nhật trạng thái Đề cương)", description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n"
                        +
                        "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ Session, Assessment, Material và CLO:\n\n"
                        +
                        "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                        "| :--- | :--- | :--- |\n" +
                        "| **DRAFT** | **Khởi tạo:** HoPDC mới tạo khung đề cương, chưa có nội dung chi tiết. | Chỉ người tạo nhìn thấy. |\n"
                        +
                        "| **IN_PROGRESS** | **Đang biên soạn:** Giảng viên đang xây dựng CLO, Session, chọn lọc học liệu và thiết kế Assessment. | Cho phép sửa nội dung chi tiết. |\n"
                        +
                        "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện, đang đợi Hội đồng/HoD phân công Reviewer thẩm định. | Khóa chỉnh sửa tạm thời. |\n"
                        +
                        "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa toàn bộ nội dung. |\n"
                        +
                        "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên bắt đầu được xem tài liệu và CLO. | Khóa vĩnh viễn (Read-only). |\n"
                        +
                        "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản), giữ lại để tra cứu lịch sử đào tạo. | Ẩn khỏi danh sách hiện hành. |\n\n")
        public ResponseObject<SyllabusResponse> updateStatus(
                        @PathVariable UUID id,
                        @PathVariable String accountId,
                        @RequestParam String status) {
                var account = accountService.getAccountById(accountId);
                SyllabusResponse response = syllabusService.getDetail(id, accountId);

                SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
                logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
                logRequest.setActionByEmail(account.getEmail()); // Truyền email vào đây

                SyllabusActionType actionType = syllabusActionLogService.mapStatusToAction(status);

                logRequest.setActionType(actionType.toString());
                logRequest.setNote("Hệ thống: Ẩn đề cương cho môn học.");

                // 4. Lưu Log
                syllabusActionLogService.createLog(logRequest);
                return ResponseObject.<SyllabusResponse>builder()
                                .data(syllabusService.updateStatus(id, status))
                                .message("Syllabus status updated to: " + status)
                                .build();
        }

        @PatchMapping("/{syllabusId}/publish")
        @Operation(
                summary = "Publish a Syllabus (Ban hành Đề cương)",
                description = "Chuyển trạng thái Đề cương từ **APPROVED** sang **PUBLISHED**. " +
                        "Trước khi ban hành, hệ thống tự động chuyển tất cả Đề cương **PUBLISHED** khác " +
                        "cùng môn học sang trạng thái **ARCHIVED**. " +
                        "Chỉ áp dụng cho Đề cương đang ở trạng thái APPROVED."
        )
        public ResponseObject<SyllabusResponse> publishSyllabus(
                        @PathVariable UUID syllabusId,
                        @AuthenticationPrincipal Jwt jwt) {
                String accountId = jwt.getClaimAsString("accountId");
                var account = accountService.getAccountById(accountId);

                SyllabusResponse response = syllabusService.publishSyllabus(syllabusId);

                SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
                logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
                logRequest.setActionByEmail(account.getEmail());
                logRequest.setActionType(SyllabusActionType.PUBLISH.toString());
                logRequest.setNote("Hệ thống: Ban hành đề cương chính thức cho môn học.");
                syllabusActionLogService.createLog(logRequest);

                return ResponseObject.<SyllabusResponse>builder()
                                .data(response)
                                .message("Syllabus published successfully")
                                .build();
        }

        @DeleteMapping("/{id}/account/{accountId}")
        @Operation(summary = "Delete syllabus")
        @PreAuthorize("hasAuthority('SYLLABUS_DELETE')")
        public ResponseObject<Void> delete(
                        @PathVariable UUID id,
                        @PathVariable String accountId,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                var account = accountService.getAccountById(accountId);
                SyllabusResponse response = syllabusService.getDetail(id, accountId);

                syllabusService.delete(id, userId);
                if (!response.getStatus().equals("DRAFT")) {
                        SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
                        logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
                        logRequest.setActionByEmail(account.getEmail()); // Truyền email vào đây
                        logRequest.setActionType(SyllabusActionType.ARCHIVE.toString());
                        logRequest.setNote("Hệ thống: Ẩn đề cương cho môn học.");

                        // 4. Lưu Log
                        syllabusActionLogService.createLog(logRequest);
                }
                return ResponseObject.<Void>builder()
                                .message("Syllabus archived successfully")
                                .build();
        }

        @PostMapping("/compare")
        public ResponseObject<CompareSyllabusResponse> compareSyllabusVersions(
                        @RequestParam("oldSyllabusId") UUID oldSyllabusId,
                        @RequestParam("newSyllabusId") UUID newSyllabusId) {
                return ResponseObject.<CompareSyllabusResponse>builder()
                        .data(embeddingService.compareTwoVersionSyllabus(oldSyllabusId, newSyllabusId))
                        .message("Compare syllabus successfully")
                        .build();
        }

        @PostMapping("/save-compare-version")
        public ResponseObject<SyllabusComparisonHistory> saveComparisonHistory(@RequestBody CompareSyllabusResponse compareSyllabusResponse) {
                SyllabusComparisonHistory result = new  SyllabusComparisonHistory();
                if(embeddingService.validateLatestAndSubsequentVersions(compareSyllabusResponse.getOldSyllabusId(), compareSyllabusResponse.getNewSyllabusId())) {
                        result = embeddingService.saveComparisonHistory(compareSyllabusResponse.getOldSyllabusId(), compareSyllabusResponse.getNewSyllabusId(), compareSyllabusResponse.getAssessmentDiffResponse(), compareSyllabusResponse.getComparisonResult(), compareSyllabusResponse.getSessionDiffResponse());
                }
                return ResponseObject.<SyllabusComparisonHistory>builder()
                        .data(result)
                        .message("Save syllabus successfully")
                        .build();
        }


        @GetMapping("/pending-review/department")
        @Operation(summary = "Get Pending Review Syllabuses by Department", description = "Lấy danh sách các Đề cương đang chờ duyệt thuộc Phòng ban của người dùng hiện tại.")
        public ResponseObject<List<SyllabusResponse>> getPendingSyllabusesByDept(
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<List<SyllabusResponse>>builder()
                                .data(syllabusService.getSyllabusesByDepartment(userId,
                                                SyllabusStatus.PENDING_REVIEW.toString()))
                                .message("Syllabuses retrieved successfully")
                                .build();
        }

        @GetMapping("/in-progress/department")
        @Operation(summary = "Get in prgress Syllabuses by Department", description = "Lấy danh sách các Đề cương đang biên soạn chi tiết nội dung Phòng ban của người dùng hiện tại.")
        public ResponseObject<List<SyllabusResponse>> getInProgressSyllabusesByDept(
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<List<SyllabusResponse>>builder()
                                .data(syllabusService.getSyllabusesByDepartment(userId,
                                                SyllabusStatus.IN_PROGRESS.toString()))
                                .message("Syllabuses retrieved successfully")
                                .build();
        }
        @PostMapping("/copy")
        @Operation(summary = "Copy data from one syllabus to another", description = "Copies Material, Blocks, Assessment, and Session from oldSyllabusId to newSyllabusId")
        public ResponseObject<Void> copySyllabusData(
                        @RequestParam("oldSyllabusId") UUID oldSyllabusId,
                        @RequestParam("newSyllabusId") UUID newSyllabusId) {
                
                syllabusService.copySyllabusData(oldSyllabusId, newSyllabusId);

                return ResponseObject.<Void>builder()
                                .status(1000)
                                .message("Syllabus data copied successfully")
                                .build();
        }

        @PutMapping("/selected-compare-syllabus")
        @Operation(summary = "Selected compare syllabus", description = "Selected compare syllabus for student diff view")
        public ResponseObject<SyllabusComparisonHistory> selectHistoryCompare(
                @RequestParam("historyId") UUID historyId) {
                embeddingService.selectHistoryCompare(historyId);
                return ResponseObject.<SyllabusComparisonHistory>builder()
                        .status(1000)
                        .data(embeddingService.selectHistoryCompare(historyId))
                        .message("Selected compare syllabus successfully")
                        .build();
        }

        @GetMapping("/{newSyllabusId}/get-syllabus-compare/HoPDC")
        @Operation(summary = "Get all compare syllabus", description = "Get all compare syllabus for student diff view")
        public ResponseObject<List<SyllabusComparisonHistory>> getHistoryCompareHoPDC(
                @PathVariable("newSyllabusId") UUID newSyllabusId) {
                return ResponseObject.<List<SyllabusComparisonHistory>>builder()
                        .status(1000)
                        .data(embeddingService.getComparisonHistoryDetailForHoPDC(newSyllabusId))
                        .message("Get all compare syllabus successfully")
                        .build();
        }

        @GetMapping("/{newSyllabusId}/get-syllabus-compare/student")
        @Operation(summary = "Get all compare syllabus", description = "Get all compare syllabus for student diff view")
        public ResponseObject<SyllabusComparisonHistory> getHistoryCompareStudent(
                @PathVariable("newSyllabusId") UUID newSyllabusId) {
                return ResponseObject.<SyllabusComparisonHistory>builder()
                        .status(1000)
                        .data(embeddingService.getComparisonHistoryDetailForStudent(newSyllabusId))
                        .message("Get all compare syllabus successfully")
                        .build();
        }
}
